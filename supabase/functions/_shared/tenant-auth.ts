import { createClient, type SupabaseClient, type User } from "https://esm.sh/@supabase/supabase-js@2"

export const corsHeaders = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, x-client-info, apikey, content-type",
}

export function jsonResponse(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...corsHeaders },
  })
}

export class TenantAuthError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export type TenantCaller = {
  user: User
  admin: SupabaseClient
  organizationId: string
  tenantId: string
  employeeRowId: string
  employeeLoginId: string
  roles: string[]
  isTenantAdmin: boolean
  isPlatformAdmin: boolean
}

export type TenantTarget = {
  id: string
  organization_id: string
  code: string
  slug: string
  name: string
  active: boolean
}

function env(name: string) {
  return String(Deno.env.get(name) || "").trim()
}

export function normalizeText(value: unknown) {
  return String(value ?? "").trim()
}

export function parseRoles(value: unknown) {
  return normalizeText(value)
    .toLowerCase()
    .split(/[,|;]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .filter((item, index, rows) => rows.indexOf(item) === index)
}

export function normalizeRolesForStore(value: unknown) {
  const roles = Array.isArray(value) ? value.flatMap(parseRoles) : parseRoles(value)
  return roles.filter((item, index, rows) => rows.indexOf(item) === index).join(",")
}

export function normalizeLoginId(value: unknown) {
  return normalizeText(value).toLowerCase().replace(/\s+/g, "")
}

export function validateStrongPassword(value: unknown) {
  const password = String(value ?? "")
  if (password.length < 12) {
    throw new TenantAuthError(400, "Password minimal 12 karakter.")
  }
  if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password) || !/[^A-Za-z0-9]/.test(password)) {
    throw new TenantAuthError(400, "Password harus memuat huruf besar, huruf kecil, angka, dan simbol.")
  }
  return password
}

export async function internalAuthEmail(tenantId: string, loginId: string) {
  const source = `${normalizeText(tenantId)}:${normalizeLoginId(loginId)}`
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(source))
  const hex = Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("")
  return `${hex}@accounts.mim.invalid`
}

export async function requireTenantCaller(req: Request): Promise<TenantCaller> {
  const supabaseUrl = env("SUPABASE_URL")
  const anonKey = env("SUPABASE_ANON_KEY")
  const serviceRoleKey = env("SUPABASE_SERVICE_ROLE_KEY")
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    throw new TenantAuthError(500, "Konfigurasi server Supabase belum lengkap.")
  }

  const authorization = normalizeText(req.headers.get("authorization"))
  const match = authorization.match(/^Bearer\s+(.+)$/i)
  if (!match) throw new TenantAuthError(401, "Sesi login tidak ditemukan.")

  const token = match[1]
  const authClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  })
  const { data: userData, error: userError } = await authClient.auth.getUser(token)
  if (userError || !userData.user) {
    throw new TenantAuthError(401, "Sesi login tidak valid atau sudah berakhir.")
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  })
  const user = userData.user
  const hasVerifiedMfa = (user.factors || []).some((factor) => factor.status === "verified")
  const tokenParts = token.split(".")
  let assuranceLevel = "aal1"
  if (tokenParts.length >= 2) {
    try {
      const encoded = tokenParts[1].replace(/-/g, "+").replace(/_/g, "/")
      const padded = encoded.padEnd(Math.ceil(encoded.length / 4) * 4, "=")
      assuranceLevel = normalizeText(JSON.parse(atob(padded))?.aal || "aal1").toLowerCase()
    } catch (_error) {}
  }
  if (hasVerifiedMfa && assuranceLevel !== "aal2") {
    throw new TenantAuthError(403, "MFA_REQUIRED")
  }

  const [employeeResult, platformResult] = await Promise.all([
    admin
      .from("karyawan")
      .select("id,id_karyawan,organization_id,tenant_id,role,aktif")
      .eq("auth_user_id", user.id)
      .maybeSingle(),
    admin
      .from("platform_admins")
      .select("organization_id,active")
      .eq("auth_user_id", user.id)
      .eq("active", true)
      .maybeSingle(),
  ])

  if (employeeResult.error) throw new TenantAuthError(500, employeeResult.error.message)
  if (platformResult.error) throw new TenantAuthError(500, platformResult.error.message)

  const employee = employeeResult.data
  const platform = platformResult.data
  const employeeActive = employee?.aktif === true
  const roles = employeeActive ? parseRoles(employee?.role) : []
  const organizationId = normalizeText(platform?.organization_id || employee?.organization_id)
  if (!organizationId || (!platform && !employeeActive)) {
    throw new TenantAuthError(403, "Akun tidak aktif atau belum terhubung ke unit.")
  }

  return {
    user,
    admin,
    organizationId,
    tenantId: employeeActive ? normalizeText(employee?.tenant_id) : "",
    employeeRowId: employeeActive ? normalizeText(employee?.id) : "",
    employeeLoginId: employeeActive ? normalizeText(employee?.id_karyawan) : "",
    roles,
    isTenantAdmin: roles.includes("admin"),
    isPlatformAdmin: platform?.active === true,
  }
}

export async function resolveTenantTarget(
  caller: TenantCaller,
  requestedTenantId: unknown,
): Promise<TenantTarget> {
  const requested = normalizeText(requestedTenantId)
  const tenantId = caller.isPlatformAdmin && requested
    ? requested
    : caller.tenantId

  if (!tenantId) throw new TenantAuthError(400, "Tenant belum dipilih.")

  const { data, error } = await caller.admin
    .from("tenants")
    .select("id,organization_id,code,slug,name,active")
    .eq("id", tenantId)
    .eq("organization_id", caller.organizationId)
    .maybeSingle()

  if (error) throw new TenantAuthError(500, error.message)
  if (!data) throw new TenantAuthError(404, "Unit tidak ditemukan.")
  return data as TenantTarget
}

export function requireAdministration(caller: TenantCaller) {
  if (!caller.isPlatformAdmin && !caller.isTenantAdmin) {
    throw new TenantAuthError(403, "Hanya admin yang dapat melakukan tindakan ini.")
  }
}

export async function writeAuditLog(
  caller: TenantCaller,
  tenantId: string | null,
  action: string,
  targetType: string,
  targetId: string | null,
  metadata: Record<string, unknown> = {},
) {
  const { error } = await caller.admin.from("tenant_audit_logs").insert({
    organization_id: caller.organizationId,
    tenant_id: tenantId,
    actor_auth_user_id: caller.user.id,
    actor_employee_id: caller.employeeRowId || null,
    action,
    target_type: targetType,
    target_id: targetId,
    metadata_json: metadata,
  })
  if (error) throw new TenantAuthError(500, error.message)
}

export async function enforceActionRateLimit(
  caller: TenantCaller,
  action: string,
  maxAttempts: number,
  windowSeconds: number,
) {
  const since = new Date(Date.now() - Math.max(1, windowSeconds) * 1000).toISOString()
  const { count, error } = await caller.admin
    .from("tenant_audit_logs")
    .select("id", { count: "exact", head: true })
    .eq("actor_auth_user_id", caller.user.id)
    .eq("action", action)
    .gte("created_at", since)
  if (error) throw new TenantAuthError(500, error.message)
  if ((count || 0) >= Math.max(1, maxAttempts)) {
    throw new TenantAuthError(429, "Terlalu banyak percobaan. Tunggu beberapa saat lalu coba lagi.")
  }
}

export function handleTenantError(error: unknown) {
  if (error instanceof TenantAuthError) {
    return jsonResponse(error.status, { ok: false, error: error.message })
  }
  const message = error instanceof Error ? error.message : String(error)
  return jsonResponse(500, { ok: false, error: message || "Terjadi kesalahan server." })
}
