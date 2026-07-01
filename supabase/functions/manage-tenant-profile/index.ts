import { serve } from "https://deno.land/std@0.224.0/http/server.ts"
import {
  corsHeaders,
  handleTenantError,
  jsonResponse,
  normalizeText,
  requireAdministration,
  requireTenantCaller,
  resolveTenantTarget,
  TenantAuthError,
  writeAuditLog,
} from "../_shared/tenant-auth.ts"

type ProfilePayload = {
  action?: string
  tenant_id?: string
  name?: string
  official_name?: string
  address?: string
  phone?: string
  email?: string
  website?: string
  logo_url?: string
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
    const tenant = await resolveTenantTarget(caller, payload.tenant_id)
    const action = normalizeText(payload.action || "get").toLowerCase()

    if (action === "get") {
      const { data, error } = await caller.admin
        .from("tenants")
        .select("id,organization_id,code,slug,name,official_name,unit_type,address,phone,email,website,logo_url,active,created_at,updated_at")
        .eq("id", tenant.id)
        .single()
      if (error) throw new TenantAuthError(500, error.message)
      return jsonResponse(200, { ok: true, profile: data })
    }

    if (action !== "update") throw new TenantAuthError(400, "Aksi profil unit tidak dikenal.")
    requireAdministration(caller)

    const name = normalizeText(payload.name)
    if (!name) throw new TenantAuthError(400, "Nama unit wajib diisi.")

    const updates = {
      name: name.slice(0, 160),
      official_name: nullableText(payload.official_name, 200),
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
    if (error) throw new TenantAuthError(500, error.message)

    const { data: structureRows, error: structureUpdateError } = await caller.admin
      .from("struktur_sekolah")
      .update({ nama_sekolah: updates.official_name || updates.name, alamat_sekolah: updates.address })
      .eq("tenant_id", tenant.id)
      .select("id")
    if (structureUpdateError) throw new TenantAuthError(500, structureUpdateError.message)

    if (!structureRows?.length) {
      const { error: structureInsertError } = await caller.admin
        .from("struktur_sekolah")
        .insert({
          id: crypto.randomUUID(),
          tenant_id: tenant.id,
          nama_sekolah: updates.official_name || updates.name,
          alamat_sekolah: updates.address,
        })
      if (structureInsertError) throw new TenantAuthError(500, structureInsertError.message)
    }

    await writeAuditLog(caller, tenant.id, "tenant.profile.update", "tenant", tenant.id, {
      fields: Object.keys(updates).filter((key) => key !== "updated_by"),
    })

    return jsonResponse(200, { ok: true, profile: data })
  } catch (error) {
    return handleTenantError(error)
  }
})
