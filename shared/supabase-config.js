;(function initSharedSupabaseClient() {
  if (window.mimSupabaseClient) return

  const url = 'https://optucpelkueqmlhwlbej.supabase.co'
  const key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9wdHVjcGVsa3VlcW1saHdsYmVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzAxOTY4MTgsImV4cCI6MjA4NTc3MjgxOH0.Vqaey9pcnltu9uRbPk0J-AGWaGDZjQLw92pcRv67GNE'
  if (!window.getSharedSupabaseClient) {
    throw new Error('Shared Supabase utility belum dimuat.')
  }

  window.MIM_SUPABASE_URL = url
  window.MIM_SUPABASE_ANON_KEY = key
  window.mimSupabaseClient = window.getSharedSupabaseClient(url, key, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: false
    }
  })
})()
