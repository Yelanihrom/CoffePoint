const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { readVersion, renderVersion, localTargets, checkLinks } = require('../docs.cjs');

test('reads literal Gradle version and rejects missing or ambiguous values', () => {
  assert.deepEqual(readVersion('  versionCode = 15\r\n  versionName = "1.0.15"\r\n'), { code: '15', name: '1.0.15' });
  assert.throws(() => readVersion('versionCode = 15'));
  assert.throws(() => readVersion('versionCode = 15\nversionCode = 16\nversionName = "test"'));
});

test('renders only marked block, preserves CRLF and is idempotent', () => {
  const source = 'Before\r\n<!-- generated:app-version:start -->\r\nold\r\n<!-- generated:app-version:end -->\r\nAfter';
  const version = { code: '16', name: '1.0.16' };
  const rendered = renderVersion(source, version);
  assert.ok(rendered.includes('versionCode = 16\r\nversionName = "1.0.16"'));
  assert.ok(rendered.startsWith('Before\r\n'));
  assert.ok(rendered.endsWith('\r\nAfter'));
  assert.equal(renderVersion(rendered, version), rendered);
  assert.throws(() => renderVersion('no markers', version));
  assert.throws(() => renderVersion(source + source, version));
  assert.throws(() => renderVersion('<!-- generated:app-version:end --><!-- generated:app-version:start -->', version));
});

test('extracts local inline, image and reference targets but skips URLs and code', () => {
  const markdown = [
    '[File](../file.md#heading)', '![Image](images/icon.webp)', '[Spaces](<some%20file.md>)',
    '[Site](https://example.com)', '[Anchor](#heading)', '[Mail](mailto:a@example.com)',
    '`[Ignored](missing.md)`', '```text', '[Example](missing.md)', '```',
    '[ref]: /docs/README.md "Documentation"',
  ].join('\n');
  assert.deepEqual(localTargets(markdown), ['../file.md', 'images/icon.webp', 'some file.md', '/docs/README.md']);
});

test('checks files and directories, reports broken and escaping links', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'coffeepoint-docs-'));
  try {
    fs.mkdirSync(path.join(root, 'docs'));
    fs.writeFileSync(path.join(root, 'README.md'), '[Directory](docs)\n[Missing](missing.md)\n[Outside](../outside.md)');
    const errors = checkLinks(root, ['README.md']);
    assert.equal(errors.length, 2);
    assert.ok(errors.some((error) => error.includes('missing link target')));
    assert.ok(errors.some((error) => error.includes('outside repository')));
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
