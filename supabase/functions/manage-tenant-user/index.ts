import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import {
  corsHeaders,
  enforceActionRateLimit,
  handleTenantError,
  internalAuthEmail,
  jsonResponse,
  normalizeLoginId,
  normalizeRolesForStore,
  normalizeText,
  requireAdministration,
  requireTenantCaller,
  resolveTenantTarget,
  TenantAuthError,
  validateStrongPassword,
  writeAuditLog,
} from "../_shared/tenant-auth.ts"

type UserPayload = {
  action?: string
  tenant_id?: string
  employee_id?: string
  login_id?: string
  name?: string
  roles?: string[] | string
  phone?: string
  address?: string
  photo_url?: string
  active?: boolean
  password?: string
}

function nullableText(value: unknown, maxLength: number) {
  const text = normalizeText(value)
  return text ? text.slice(0, maxLength) : null
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })
  if (req.method !== "POST") return jsonResponse(405, { ok: false, error: "Method not allowed" })

  try {
    const payload = await req.json() as UserPayload
    const caller = await requireTenantCaller(req)
    requireAdministration(caller)
    const tenant = await resolveTenantTarget(caller, payload.tenant_id)
    const action = normalizeText(payload.action || "list").toLowerCase()

    if (action === "list") {
      const [{ data, error }, resetResult] = await Promise.all([
        caller.admin
        .from("karyawan")
        .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,foto_url,auth_user_id,must_change_password,last_login_at")
        .eq("tenant_id", tenant.id)
        .order("aktif", { ascending: false })
        .order("nama", { ascending: true }),
        caller.admin
          .from("password_reset_requests")
          .select("employee_id,requested_at")
          .eq("tenant_id", tenant.id)
          .eq("status", "pending"),
      ])
      if (error) throw new TenantAuthError(500, error.message)
      if (resetResult.error) throw new TenantAuthError(500, resetResult.error.message)
      const resetByEmployee = new Map(
        (resetResult.data || []).map(row => [String(row.employee_id), String(row.requested_at || "")]),
      )
      const employees = (data || []).map(employee => ({
        ...employee,
        password_reset_requested_at: resetByEmployee.get(String(employee.id)) || null,
      }))
      return jsonResponse(200, { ok: true, employees })
    }

    if (action === "create") {
      const loginId = normalizeLoginId(payload.login_id)
      const name = normalizeText(payload.name)
      const roles = normalizeRolesForStore(payload.roles)
      const password = validateStrongPassword(payload.password)
      if (!loginId || !name || !roles) {
        throw new TenantAuthError(400, "ID Karyawan, nama, dan role wajib diisi.")
      }

      const { data: existing, error: existingError } = await caller.admin
        .from("karyawan")
        .select("id")
        .eq("tenant_id", tenant.id)
        .ilike("id_karyawan", loginId)
        .maybeSingle()
      if (existingError) throw new TenantAuthError(500, existingError.message)
      if (existing) throw new TenantAuthError(409, "ID Karyawan sudah digunakan di unit ini.")

      const authEmail = await internalAuthEmail(tenant.id, loginId)
      const { data: authData, error: authError } = await caller.admin.auth.admin.createUser({
        email: authEmail,
        password,
        email_confirm: true,
        user_metadata: { tenant_id: tenant.id, employee_id: loginId },
      })
      if (authError || !authData.user) {
        throw new TenantAuthError(400, authError?.message || "Gagal membuat akun Auth.")
      }

      const employeeId = crypto.randomUUID()
      const { data, error } = await caller.admin
        .from("karyawan")
        .insert({
          id: employeeId,
          id_karyawan: loginId,
          nama: name.slice(0, 200),
          role: roles,
          no_hp: nullableText(payload.phone, 40),
          alamat: nullableText(payload.address, 1000),
          foto_url: nullableText(payload.photo_url, 1000),
          aktif: payload.active !== false,
          organization_id: tenant.organization_id,
          tenant_id: tenant.id,
          auth_user_id: authData.user.id,
          must_change_password: true,
        })
        .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,foto_url,auth_user_id,must_change_password")
        .single()

      if (error) {
        await caller.admin.auth.admin.deleteUser(authData.user.id).catch(() => undefined)
        throw new TenantAuthError(500, error.message)
      }

      await writeAuditLog(caller, tenant.id, "tenant.employee.create", "karyawan", employeeId, {
        login_id: loginId,
        roles,
      })
      return jsonResponse(201, { ok: true, employee: data })
    }

    const employeeId = normalizeText(payload.employee_id)
    if (!employeeId) throw new TenantAuthError(400, "employee_id wajib diisi.")

    const { data: employee, error: employeeError } = await caller.admin
      .from("karyawan")
      .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,auth_user_id")
      .eq("id", employeeId)
      .eq("tenant_id", tenant.id)
      .maybeSingle()
    if (employeeError) throw new TenantAuthError(500, employeeError.message)
    if (!employee) throw new TenantAuthError(404, "Data karyawan tidak ditemukan.")

    if (action === "reset_password") {
      await enforceActionRateLimit(caller, "tenant.employee.password.reset", 10, 3600)
      const password = validateStrongPassword(payload.password)
      if (!employee.auth_user_id) {
        throw new TenantAuthError(409, "Akun ini belum terhubung ke Supabase Auth.")
      }
      const { error } = await caller.admin.auth.admin.updateUserById(employee.auth_user_id, { password })
      if (error) throw new TenantAuthError(500, error.message)
      await caller.admin
        .from("karyawan")
        .update({ must_change_password: true, password_changed_at: null })
        .eq("id", employee.id)
      await caller.admin
        .from("password_reset_requests")
        .update({ status: "resolved", resolved_at: new Date().toISOString(), resolved_by: caller.employeeRowId || null })
        .eq("employee_id", employee.id)
        .eq("tenant_id", tenant.id)
        .eq("status", "pending")
      await writeAuditLog(caller, tenant.id, "tenant.employee.password.reset", "karyawan", employee.id)
      return jsonResponse(200, { ok: true })
    }

    if (action === "migrate_auth") {
      const password = validateStrongPassword(payload.password)
      if (employee.auth_user_id) {
        throw new TenantAuthError(409, "Akun ini sudah terhubung ke Supabase Auth.")
      }

      const loginId = normalizeLoginId(employee.id_karyawan)
      const authEmail = await internalAuthEmail(tenant.id, loginId)
      const { data: authData, error: authError } = await caller.admin.auth.admin.createUser({
        email: authEmail,
        password,
        email_confirm: true,
        user_metadata: { tenant_id: tenant.id, employee_id: loginId },
      })
      if (authError || !authData.user) {
        throw new TenantAuthError(400, authError?.message || "Gagal membuat akun Auth.")
      }

      const { data, error } = await caller.admin
        .from("karyawan")
        .update({
          auth_user_id: authData.user.id,
          must_change_password: true,
        })
        .eq("id", employee.id)
        .eq("tenant_id", tenant.id)
        .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,auth_user_id,must_change_password")
        .single()

      if (error) {
        await caller.admin.auth.admin.deleteUser(authData.user.id).catch(() => undefined)
        throw new TenantAuthError(500, error.message)
      }

      await writeAuditLog(caller, tenant.id, "tenant.employee.auth.migrate", "karyawan", employee.id, {
        login_id: loginId,
      })
      return jsonResponse(200, { ok: true, employee: data })
    }

    if (action === "set_active") {
      const active = payload.active === true
      const { data, error } = await caller.admin
        .from("karyawan")
        .update({ aktif: active })
        .eq("id", employee.id)
        .eq("tenant_id", tenant.id)
        .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,auth_user_id,must_change_password")
        .single()
      if (error) throw new TenantAuthError(500, error.message)
      await writeAuditLog(caller, tenant.id, "tenant.employee.active.update", "karyawan", employee.id, { active })
      return jsonResponse(200, { ok: true, employee: data })
    }

    if (action === "update") {
      const loginId = normalizeLoginId(payload.login_id || employee.id_karyawan)
      const name = normalizeText(payload.name || employee.nama)
      const roles = normalizeRolesForStore(payload.roles || employee.role)
      if (!loginId || !name || !roles) {
        throw new TenantAuthError(400, "ID Karyawan, nama, dan role wajib diisi.")
      }

      if (loginId !== normalizeLoginId(employee.id_karyawan)) {
        const { data: duplicate, error: duplicateError } = await caller.admin
          .from("karyawan")
          .select("id")
          .eq("tenant_id", tenant.id)
          .ilike("id_karyawan", loginId)
          .neq("id", employee.id)
          .maybeSingle()
        if (duplicateError) throw new TenantAuthError(500, duplicateError.message)
        if (duplicate) throw new TenantAuthError(409, "ID Karyawan sudah digunakan di unit ini.")

        if (employee.auth_user_id) {
          const nextAuthEmail = await internalAuthEmail(tenant.id, loginId)
          const { error: authError } = await caller.admin.auth.admin.updateUserById(employee.auth_user_id, {
            email: nextAuthEmail,
            email_confirm: true,
            user_metadata: { tenant_id: tenant.id, employee_id: loginId },
          })
          if (authError) throw new TenantAuthError(500, authError.message)
        }
      }

      const updates: Record<string, unknown> = {
        id_karyawan: loginId,
        nama: name.slice(0, 200),
        role: roles,
        no_hp: nullableText(payload.phone, 40),
        alamat: nullableText(payload.address, 1000),
      }
      if (Object.prototype.hasOwnProperty.call(payload, "photo_url")) {
        updates.foto_url = nullableText(payload.photo_url, 1000)
      }

      const { data, error } = await caller.admin
        .from("karyawan")
        .update(updates)
        .eq("id", employee.id)
        .eq("tenant_id", tenant.id)
        .select("id,id_karyawan,nama,role,no_hp,alamat,aktif,foto_url,auth_user_id,must_change_password")
        .single()
      if (error) throw new TenantAuthError(500, error.message)

      await writeAuditLog(caller, tenant.id, "tenant.employee.update", "karyawan", employee.id, {
        login_id: loginId,
        roles,
      })
      return jsonResponse(200, { ok: true, employee: data })
    }

    throw new TenantAuthError(400, "Aksi akun unit tidak dikenal.")
  } catch (error) {
    return handleTenantError(error)
  }
})
