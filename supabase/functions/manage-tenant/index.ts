import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import {
  corsHeaders,
  handleTenantError,
  jsonResponse,
  normalizeText,
  requireTenantCaller,
  resolveTenantTarget,
  TenantAuthError,
  writeAuditLog,
} from "../_shared/tenant-auth.ts"

type TenantPayload = {
  action?: string
  tenant_id?: string
  code?: string
  slug?: string
  name?: string
  official_name?: string
  unit_type?: string
  address?: string
  phone?: string
  email?: string
  website?: string
  logo_url?: string
  active?: boolean
}

function requirePlatformAdmin(isPlatformAdmin: boolean) {
  if (!isPlatformAdmin) {
    throw new TenantAuthError(403, "Hanya platform admin yang dapat mengelola unit.")
  }
}

function normalizedIdentifier(value: unknown, label: string) {
  const normalized = normalizeText(value)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
  if (!normalized) throw new TenantAuthError(400, `${label} wajib diisi.`)
  return normalized.slice(0, 80)
}

function nullableText(value: unknown, maxLength: number) {
  const text = normalizeText(value)
  return text ? text.slice(0, maxLength) : null
}

serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })
  if (req.method !== "POST") return jsonResponse(405, { ok: false, error: "Method not allowed" })

  try {
    const payload = await req.json() as TenantPayload
    const caller = await requireTenantCaller(req)
    requirePlatformAdmin(caller.isPlatformAdmin)
    const action = normalizeText(payload.action || "list").toLowerCase()

    if (action === "list") {
      const { data, error } = await caller.admin
        .from("tenants")
        .select("id,organization_id,code,slug,name,official_name,unit_type,address,phone,email,website,logo_url,active,created_at,updated_at")
        .eq("organization_id", caller.organizationId)
        .order("active", { ascending: false })
        .order("name", { ascending: true })
      if (error) throw new TenantAuthError(500, error.message)
      return jsonResponse(200, { ok: true, tenants: data || [] })
    }

    if (action === "create") {
      const code = normalizedIdentifier(payload.code, "Kode unit")
      const slug = normalizedIdentifier(payload.slug || code, "Slug unit")
      const name = normalizeText(payload.name)
      if (!name) throw new TenantAuthError(400, "Nama unit wajib diisi.")

      const { data, error } = await caller.admin
        .from("tenants")
        .insert({
          organization_id: caller.organizationId,
          code,
          slug,
          name: name.slice(0, 160),
          official_name: nullableText(payload.official_name, 200),
          unit_type: nullableText(payload.unit_type, 80) || "school",
          address: nullableText(payload.address, 1000),
          phone: nullableText(payload.phone, 40),
          email: nullableText(payload.email, 160),
          website: nullableText(payload.website, 300),
          logo_url: nullableText(payload.logo_url, 1000),
          active: payload.active !== false,
          created_by: caller.user.id,
          updated_by: caller.user.id,
        })
        .select("id,organization_id,code,slug,name,official_name,unit_type,address,phone,email,website,logo_url,active,created_at,updated_at")
        .single()
      if (error) {
        if (error.code === "23505") throw new TenantAuthError(409, "Kode atau slug unit sudah digunakan.")
        throw new TenantAuthError(500, error.message)
      }

      await writeAuditLog(caller, data.id, "platform.tenant.create", "tenant", data.id, { code, slug })
      return jsonResponse(201, { ok: true, tenant: data })
    }

    const tenant = await resolveTenantTarget(caller, payload.tenant_id)

    if (action === "set_active") {
      const active = payload.active === true
      const { data, error } = await caller.admin
        .from("tenants")
        .update({ active, updated_by: caller.user.id })
        .eq("id", tenant.id)
        .eq("organization_id", caller.organizationId)
        .select("id,organization_id,code,slug,name,official_name,unit_type,address,phone,email,website,logo_url,active,updated_at")
        .single()
      if (error) throw new TenantAuthError(500, error.message)
      await writeAuditLog(caller, tenant.id, "platform.tenant.active.update", "tenant", tenant.id, { active })
      return jsonResponse(200, { ok: true, tenant: data })
    }

    if (action === "update") {
      const code = normalizedIdentifier(payload.code || tenant.code, "Kode unit")
      const slug = normalizedIdentifier(payload.slug || tenant.slug, "Slug unit")
      const name = normalizeText(payload.name || tenant.name)
      const updates = {
        code,
        slug,
        name: name.slice(0, 160),
        official_name: nullableText(payload.official_name, 200),
        unit_type: nullableText(payload.unit_type, 80) || "school",
        address: nullableText(payload.address, 1000),
        phone: nullableText(payload.phone, 40),
        email: nullableText(payload.email, 160),
        website: nullableText(payload.website, 300),
        logo_url: nullableText(payload.logo_url, 1000),
        updated_by: caller.user.id,
      }
      const { data, error } = await caller.admin
        .from("tenants")
        .update(updates)
        .eq("id", tenant.id)
        .eq("organization_id", caller.organizationId)
        .select("id,organization_id,code,slug,name,official_name,unit_type,address,phone,email,website,logo_url,active,updated_at")
        .single()
      if (error) {
        if (error.code === "23505") throw new TenantAuthError(409, "Kode atau slug unit sudah digunakan.")
        throw new TenantAuthError(500, error.message)
      }
      await writeAuditLog(caller, tenant.id, "platform.tenant.update", "tenant", tenant.id, { code, slug })
      return jsonResponse(200, { ok: true, tenant: data })
    }

    throw new TenantAuthError(400, "Aksi pengelolaan unit tidak dikenal.")
  } catch (error) {
    return handleTenantError(error)
  }
})
