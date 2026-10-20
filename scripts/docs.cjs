const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const START = '<!-- generated:app-version:start -->';
const END = '<!-- generated:app-version:end -->';

function readVersion(source) {
  const code = [...source.matchAll(/^\s*versionCode\s*=\s*(\d+)\s*$/gm)];
  const name = [...source.matchAll(/^\s*versionName\s*=\s*"([^"\r\n]+)"\s*$/gm)];
  if (code.length !== 1 || name.length !== 1) {
    throw new Error('Expected one literal versionCode and versionName in app/build.gradle.kts.');
  }
  return { code: code[0][1], name: name[0][1] };
}

function renderVersion(document, version) {
  if (document.split(START).length !== 2 || document.split(END).length !== 2) {
    throw new Error('Expected exactly one app-version marker pair in docs/releases.md.');
  }
  const start = document.indexOf(START) + START.length;
  const end = document.indexOf(END);
  if (end < start) throw new Error('App-version markers are reversed.');
  const eol = document.includes('\r\n') ? '\r\n' : '\n';
  const block = [
    '', '```kotlin', `versionCode = ${version.code}`, `versionName = "${version.name}"`, '```', '',
  ].join(eol);
  return document.slice(0, start) + block + document.slice(end);
}

function localTargets(markdown) {
  const withoutCode = markdown.replace(/^\s*(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\s*\1\s*$/gm, '')
    .replace(/`[^`\n]+`/g, '');
  const targets = [];
  const add = (target) => {
    if (/^(?:[a-z][a-z\d+.-]*:|\/\/|#)/i.test(target)) return;
    const file = target.split(/[?#]/, 1)[0];
    if (file) targets.push(decodeURIComponent(file));
  };
  for (const match of withoutCode.matchAll(/!?\[[^\]\n]*\]\(\s*(<[^>\n]+>|[^\s)]+)(?:\s+["'][^\n]*?["'])?\s*\)/g)) {
    add(match[1].replace(/^<|>$/g, ''));
  }
  for (const match of withoutCode.matchAll(/^ {0,3}\[[^\]\n]+\]:\s*(<[^>\n]+>|\S+)/gm)) {
    add(match[1].replace(/^<|>$/g, ''));
  }
  return targets;
}

function checkLinks(root, files) {
  const errors = [];
  for (const file of files) {
    for (const target of localTargets(fs.readFileSync(path.join(root, file), 'utf8'))) {
      const resolved = target.startsWith('/')
        ? path.resolve(root, '.' + target)
        : path.resolve(root, path.dirname(file), target);
      const relative = path.relative(root, resolved);
      if (relative === '..' || relative.startsWith('..' + path.sep) || path.isAbsolute(relative)) {
        errors.push(`${file}: link outside repository: ${target}`);
      } else if (!fs.existsSync(resolved)) {
        errors.push(`${file}: missing link target: ${target}`);
      }
    }
  }
  return errors;
}

function main(args) {
  if (args.length !== 1 || !['--write', '--check'].includes(args[0])) {
    throw new Error('Usage: node scripts/docs.cjs --write | --check');
  }
  const root = path.resolve(__dirname, '..');
  const releasePath = path.join(root, 'docs/releases.md');
  const current = fs.readFileSync(releasePath, 'utf8');
  const expected = renderVersion(current, readVersion(fs.readFileSync(path.join(root, 'app/build.gradle.kts'), 'utf8')));
  if (args[0] === '--write') {
    if (expected !== current) fs.writeFileSync(releasePath, expected);
    console.log('Version documentation is up to date.');
    return;
  }
  const files = [...new Set(execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], {
    cwd: root, encoding: 'utf8',
  }).split('\0').filter((file) => /\.md$/i.test(file)))];
  const errors = checkLinks(root, files);
  if (current !== expected) errors.unshift('Stale version in docs/releases.md. Run: node scripts/docs.cjs --write');
  if (errors.length) throw new Error(errors.join('\n'));
  console.log(`Documentation OK: version matches Gradle; local file links checked in ${files.length} Markdown files.`);
}

module.exports = { readVersion, renderVersion, localTargets, checkLinks };
if (require.main === module) {
  try { main(process.argv.slice(2)); } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
