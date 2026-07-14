import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import {
  corsHeaders,
  enforceActionRateLimit,
  handleTenantError,
  jsonResponse,
  normalizeText,
  requireTenantCaller,
  TenantAuthError,
  validateStrongPassword,
  writeAuditLog,
} from "../_shared/tenant-auth.ts"

type ProfilePayload = {
  action?: string
  name?: string
  phone?: string
  address?: string
  avatar_url?: string
  password?: string
  current_password?: string
  new_password?: string
}

async function getGoogleIdentityInfo(caller: Awaited<ReturnType<typeof requireTenantCaller>>) {
  let identities = caller.user.identities || []
  if (!identities.length) {
    const { data, error } = await caller.admin.auth.admin.getUserById(caller.user.id)
    if (!error && data?.user?.identities) identities = data.user.identities
  }
  const googleIdentity = identities.find((identity) => String(identity.provider || "").toLowerCase() === "google")
  const identityData = googleIdentity?.identity_data as Record<string, unknown> | undefined
  return {
    google_linked: Boolean(googleIdentity),
    google_email: normalizeText(identityData?.email || ""),
  }
}

async function profileResponse(caller: Awaited<ReturnType<typeof requireTenantCaller>>, profile: Record<string, unknown>) {
  return {
    ...profile,
    ...await getGoogleIdentityInfo(caller),
  }
}

function nullableText(value: unknown, maxLength: number) {
  const text = normalizeText(value)
  return text ? text.slice(0, maxLength) : null
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })
  if (req.method !== "POST") return jsonResponse(405, { ok: false, error: "Method not allowed" })

  try {
    const payload = await req.json() as ProfilePayload
    const caller = await requireTenantCaller(req)
    if (!caller.employeeRowId || !caller.tenantId) {
      throw new TenantAuthError(403, "Akun ini tidak terhubung ke profil karyawan aktif.")
    }
    const action = normalizeText(payload.action || "get").toLowerCase()

    if (action === "get") {
      const { data, error } = await caller.admin
        .from("karyawan")
        .select("id,id_karyawan,nama,no_hp,alamat,foto_url,tenant_id,auth_user_id,must_change_password")
        .eq("id", caller.employeeRowId)
        .eq("tenant_id", caller.tenantId)
        .single()
      if (error) throw new TenantAuthError(500, error.message)
      return jsonResponse(200, { ok: true, profile: await profileResponse(caller, data) })
    }

    if (action === "change_password") {
      await enforceActionRateLimit(caller, "tenant.employee.password.change.attempt", 5, 900)
      await writeAuditLog(
        caller,
        caller.tenantId,
        "tenant.employee.password.change.attempt",
        "karyawan",
        caller.employeeRowId,
      )
      const currentPassword = String(payload.current_password ?? "")
      const newPassword = validateStrongPassword(payload.new_password)
      if (!currentPassword) throw new TenantAuthError(400, "Password saat ini wajib diisi.")
      if (currentPassword === newPassword) {
        throw new TenantAuthError(400, "Password baru harus berbeda dari password saat ini.")
      }
      if (!caller.user.email) throw new TenantAuthError(400, "Identitas akun Auth tidak lengkap.")

      const verifier = createClient(
        String(Deno.env.get("SUPABASE_URL") || ""),
        String(Deno.env.get("SUPABASE_ANON_KEY") || ""),
        { auth: { persistSession: false, autoRefreshToken: false } },
      )
      const { error: verifyError } = await verifier.auth.signInWithPassword({
        email: caller.user.email,
        password: currentPassword,
      })
      if (verifyError) throw new TenantAuthError(400, "Password saat ini salah.")

      const { error: passwordError } = await caller.admin.auth.admin.updateUserById(caller.user.id, {
        password: newPassword,
      })
      if (passwordError) throw new TenantAuthError(500, passwordError.message)

      const changedAt = new Date().toISOString()
      const { error: employeeError } = await caller.admin
        .from("karyawan")
        .update({ must_change_password: false, password_changed_at: changedAt })
        .eq("id", caller.employeeRowId)
        .eq("tenant_id", caller.tenantId)
      if (employeeError) throw new TenantAuthError(500, employeeError.message)

      await writeAuditLog(
        caller,
        caller.tenantId,
        "tenant.employee.password.change",
        "karyawan",
        caller.employeeRowId,
      )
      return jsonResponse(200, { ok: true, changed_at: changedAt })
    }

    if (action !== "update") throw new TenantAuthError(400, "Aksi profil tidak dikenal.")
    const name = normalizeText(payload.name)
    if (!name) throw new TenantAuthError(400, "Nama wajib diisi.")
    if (String(payload.password ?? "")) {
      throw new TenantAuthError(400, "Gunakan menu Atur Password untuk mengganti password.")
    }

    const updates: Record<string, unknown> = {
      nama: name.slice(0, 200),
      no_hp: nullableText(payload.phone, 40),
      alamat: nullableText(payload.address, 1000),
      foto_url: nullableText(payload.avatar_url, 1000),
    }
    const { data, error } = await caller.admin
      .from("karyawan")
      .update(updates)
      .eq("id", caller.employeeRowId)
      .eq("tenant_id", caller.tenantId)
      .select("id,id_karyawan,nama,no_hp,alamat,foto_url,tenant_id,auth_user_id,must_change_password")
      .single()
    if (error) throw new TenantAuthError(500, error.message)

    await writeAuditLog(caller, caller.tenantId, "tenant.employee.profile.update", "karyawan", caller.employeeRowId, {
      password_changed: false,
    })
    return jsonResponse(200, { ok: true, profile: await profileResponse(caller, data) })
  } catch (error) {
    return handleTenantError(error)
  }
})
