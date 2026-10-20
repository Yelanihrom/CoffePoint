const { test } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { readCertificate, validateRelease, createUpdateManifest } = require('../prepare-release.cjs');

const certificate = 'a'.repeat(64);
const signing = `Signer #1 certificate SHA-256 digest: ${certificate}\n`;
const badge = (code = 16, name = '1.0.16', id = 'ru.coffeepoint') =>
  `package: name='${id}' versionCode='${code}' versionName='${name}'\nminSdkVersion:'26'\n`;
const fixture = () => ({
  tag: 'v1.0.16', gradle: 'versionCode = 16\nversionName = "1.0.16"\n',
  metadata: { variantName: 'release', applicationId: 'ru.coffeepoint', elements: [
    { versionCode: 16, versionName: '1.0.16', outputFile: 'app-release.apk' },
  ] },
  badging: badge(), signing, previousBadging: badge(15, '1.0.15'), previousSigning: signing,
  profile: { applicationId: 'ru.coffeepoint', certificateSha256: certificate },
});

test('validates a signed production upgrade', () => {
  const apk = validateRelease(fixture());
  assert.equal(apk.code, 16);
  assert.equal(apk.certificate, certificate);
  assert.equal(apk.minSdk, 26);
  assert.equal(validateRelease({ ...fixture(), badging: badge().replace('minSdkVersion', 'sdkVersion') }).minSdk, 26);
});

test('rejects wrong tag, old version, CI APK and mismatching metadata', () => {
  for (const modify of [
    (x) => { x.tag = 'v1.0.15'; },
    (x) => { x.previousBadging = badge(16); },
    (x) => { x.previousBadging = badge(17, '1.0.17'); },
    (x) => { x.badging = badge(16, '1.0.16-ci', 'ru.coffeepoint.ci'); },
    (x) => { x.metadata.variantName = 'debug'; },
    (x) => { x.metadata.elements[0].versionCode = 15; },
    (x) => { x.metadata.elements[0].outputFile = '../app.apk'; },
  ]) { const input = fixture(); modify(input); assert.throws(() => validateRelease(input)); }
});

test('rejects changed, missing, or multiple signing certificates', () => {
  const input = fixture();
  assert.throws(() => validateRelease({ ...input, signing: signing.replace(certificate, 'b'.repeat(64)) }));
  assert.throws(() => validateRelease({ ...input, previousSigning: signing.replace(certificate, 'b'.repeat(64)) }));
  assert.throws(() => readCertificate(''));
  assert.throws(() => readCertificate(signing + signing));
});

test('manifest binds APK hash, version, identity, commit and stable download URL', () => {
  const apk = validateRelease(fixture());
  const bytes = Buffer.from('test APK bytes');
  const input = { apk, repository: 'Yelanihrom/CoffePoint', tag: 'v1.0.16', commit: 'b'.repeat(40), bytes };
  const manifest = createUpdateManifest(input);
  assert.equal(manifest.versionCode, 16);
  assert.equal(manifest.apkSize, bytes.length);
  assert.equal(manifest.sha256, crypto.createHash('sha256').update(bytes).digest('hex'));
  assert.equal(manifest.apkUrl, 'https://github.com/Yelanihrom/CoffePoint/releases/download/v1.0.16/CoffeePoint-1.0.16.apk');
  assert.equal(manifest.signingCertificateSha256, certificate);
  assert.throws(() => createUpdateManifest({ ...input, commit: 'not-a-sha' }));
  assert.throws(() => createUpdateManifest({ ...input, repository: '../repository' }));
  assert.throws(() => createUpdateManifest({ ...input, bytes: Buffer.alloc(0) }));
});
