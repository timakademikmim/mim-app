import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const root = path.resolve(__dirname, '..')
const outDir = path.join(root, 'dist-tauri-web')

const removeDir = dir => {
  if (fs.existsSync(dir)) fs.rmSync(dir, { recursive: true, force: true })
}

const ensureDir = dir => {
  fs.mkdirSync(dir, { recursive: true })
}

const copyFileIfExists = (src, dst) => {
  if (!fs.existsSync(src)) return
  ensureDir(path.dirname(dst))
  fs.copyFileSync(src, dst)
}

const copyDirRecursive = (srcDir, dstDir) => {
  if (!fs.existsSync(srcDir)) return
  ensureDir(dstDir)
  for (const entry of fs.readdirSync(srcDir, { withFileTypes: true })) {
    const src = path.join(srcDir, entry.name)
    const dst = path.join(dstDir, entry.name)
    if (entry.isDirectory()) copyDirRecursive(src, dst)
    else if (entry.isFile()) copyFileIfExists(src, dst)
  }
}

removeDir(outDir)
ensureDir(outDir)

const allowedRootExt = new Set(['.html', '.js', '.css', '.ttf', '.png'])
for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
  if (!entry.isFile()) continue
  const ext = path.extname(entry.name).toLowerCase()
  if (!allowedRootExt.has(ext)) continue
  copyFileIfExists(path.join(root, entry.name), path.join(outDir, entry.name))
}

copyDirRecursive(path.join(root, 'pages'), path.join(outDir, 'pages'))
copyDirRecursive(path.join(root, 'shared'), path.join(outDir, 'shared'))

console.log(`Desktop assets built at: ${outDir}`)

