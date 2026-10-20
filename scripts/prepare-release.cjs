const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { parseArgs } = require('node:util');
const { readVersion } = require('./docs.cjs');

function readBadging(text) {
  const name = text.match(/^package: name='([^']+)'[^\n]*\bversionCode='(\d+)'[^\n]*\bversionName='([^']+)'/m);
  const minSdk = text.match(/^(?:minSdkVersion|sdkVersion):'(\d+)'/m);
  if (!name || !minSdk) throw new Error('Missing APK identity or minimum SDK in aapt2 output.');
  return { applicationId: name[1], code: Number(name[2]), name: name[3], minSdk: Number(minSdk[1]) };
}

function readCertificate(text) {
  const matches = [...text.matchAll(/^Signer #\d+ certificate SHA-256 digest: ([a-f\d]{64})\s*$/gmi)];
  if (matches.length !== 1) throw new Error('Expected one verified APK signer certificate.');
  return matches[0][1].toLowerCase();
}

function validateRelease({ tag, gradle, metadata, badging, signing, previousBadging, previousSigning, profile }) {
  const version = readVersion(gradle);
  if (!/^\d+\.\d+\.\d+$/.test(version.name) || tag !== `v${version.name}`) {
    throw new Error('Stable tag must equal v + versionName in Gradle.');
  }
  const apk = readBadging(badging);
  const previous = readBadging(previousBadging);
  const certificate = readCertificate(signing);
  const oldCertificate = readCertificate(previousSigning);
  const element = metadata.elements?.[0];
  if (profile.applicationId !== 'ru.coffeepoint' || metadata.variantName !== 'release' ||
      metadata.applicationId !== profile.applicationId || apk.applicationId !== profile.applicationId ||
      previous.applicationId !== profile.applicationId || metadata.elements?.length !== 1) {
    throw new Error('Expected a single production release APK, not a CI/debug variant.');
  }
  if (element.versionCode !== Number(version.code) || apk.code !== Number(version.code) ||
      element.versionName !== version.name || apk.name !== version.name) {
    throw new Error('APK identity does not match Gradle/output metadata.');
  }
  if (!Number.isSafeInteger(apk.code) || apk.code <= previous.code) {
    throw new Error('versionCode must be greater than the latest published APK.');
  }
  if (!/^[a-f\d]{64}$/.test(profile.certificateSha256) || certificate !== profile.certificateSha256 ||
      certificate !== oldCertificate) {
    throw new Error('Signing certificate differs from the pinned or latest published certificate.');
  }
  if (!element.outputFile || path.basename(element.outputFile) !== element.outputFile) {
    throw new Error('Invalid APK filename in output metadata.');
  }
  return { ...apk, certificate, outputFile: element.outputFile };
}

function createUpdateManifest({ apk, repository, tag, commit, bytes }) {
  if (!/^[a-z\d](?:[a-z\d-]*[a-z\d])?\/[\w.-]+$/i.test(repository) ||
      ['.', '..'].includes(repository.split('/')[1]) || !/^[a-f\d]{40}$/.test(commit)) {
    throw new Error('Expected a GitHub owner/repository and full source commit SHA.');
  }
  if (tag !== `v${apk.name}` || !/^\d+\.\d+\.\d+$/.test(apk.name)) throw new Error('Invalid release version.');
  if (!bytes.length) throw new Error('APK is empty.');
  const apkName = `CoffeePoint-${apk.name}.apk`;
  return {
    schemaVersion: 1,
    applicationId: apk.applicationId,
    versionCode: apk.code,
    versionName: apk.name,
    minSdk: apk.minSdk,
    apkName,
    apkUrl: `https://github.com/${repository}/releases/download/${tag}/${apkName}`,
    apkSize: bytes.length,
    sha256: crypto.createHash('sha256').update(bytes).digest('hex'),
    signingCertificateSha256: apk.certificate,
    sourceCommit: commit,
    releaseUrl: `https://github.com/${repository}/releases/tag/${tag}`,
  };
}

function main() {
  const { values } = parseArgs({ options: Object.fromEntries([
    'tag', 'repository', 'commit', 'certificate', 'badging', 'previous-certificate', 'previous-badging', 'out',
  ].map((key) => [key, { type: 'string' }])) });
  for (const key of ['tag', 'repository', 'commit', 'certificate', 'badging', 'previous-certificate', 'previous-badging', 'out']) {
    if (!values[key]) throw new Error(`Missing --${key}.`);
  }
  const root = path.resolve(__dirname, '..');
  const output = path.join(root, 'app/build/outputs/apk/release');
  const apk = validateRelease({
    tag: values.tag,
    gradle: fs.readFileSync(path.join(root, 'app/build.gradle.kts'), 'utf8'),
    metadata: JSON.parse(fs.readFileSync(path.join(output, 'output-metadata.json'), 'utf8')),
    badging: fs.readFileSync(values.badging, 'utf8'),
    signing: fs.readFileSync(values.certificate, 'utf8'),
    previousBadging: fs.readFileSync(values['previous-badging'], 'utf8'),
    previousSigning: fs.readFileSync(values['previous-certificate'], 'utf8'),
    profile: JSON.parse(fs.readFileSync(path.join(root, 'config/release-signing.json'), 'utf8')),
  });
  const bytes = fs.readFileSync(path.join(output, apk.outputFile));
  const manifest = createUpdateManifest({ apk, repository: values.repository, tag: values.tag, commit: values.commit, bytes });
  const destination = path.resolve(values.out);
  if (fs.existsSync(destination) && fs.readdirSync(destination).length) throw new Error('Output directory must be empty.');
  fs.mkdirSync(destination, { recursive: true });
  fs.writeFileSync(path.join(destination, manifest.apkName), bytes);
  fs.writeFileSync(path.join(destination, 'update.json'), JSON.stringify(manifest, null, 2) + '\n');
  console.log(`Prepared ${manifest.apkName} and update.json; signature and version compatibility verified.`);
}

module.exports = { readBadging, readCertificate, validateRelease, createUpdateManifest };
if (require.main === module) {
  try { main(); } catch (error) { console.error(error.message); process.exitCode = 1; }
}
