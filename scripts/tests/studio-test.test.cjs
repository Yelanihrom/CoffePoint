const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { androidTestProperties } = require('../configure-studio-test.cjs');
const root = path.resolve(__dirname, '../..');
const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));

test('local Studio settings use only the test deployment', () => {
  const result = androidTestProperties(environment, 'test-token');
  assert.ok(result.includes(`/s/${environment.deploymentId}/exec`));
  assert.ok(result.includes('syncToken=test-token\n'));
  assert.ok(fs.readFileSync(path.join(root, '.gitignore'), 'utf8').includes('/test-backend.properties'));
});

test('rejects invalid environments, deployments and tokens', () => {
  assert.throws(() => androidTestProperties({ ...environment, environment: 'production' }, 'test-token'));
  assert.throws(() => androidTestProperties({ ...environment, deploymentId: '' }, 'test-token'));
  assert.throws(() => androidTestProperties(environment, ''));
  assert.throws(() => androidTestProperties(environment, 'token\ninjected=value'));
});

test('Studio debug is isolated by default', () => {
  assert.match(fs.readFileSync(path.join(root, 'gradle.properties'), 'utf8'), /^ciBuild=true$/m);
  const gradle = fs.readFileSync(path.join(root, 'app/build.gradle.kts'), 'utf8');
  assert.ok(gradle.includes('applicationIdSuffix = ".ci"'));
  assert.ok(gradle.includes('manifestPlaceholders["appLabel"] = "CoffeePoint TEST"'));
  assert.ok(gradle.includes('manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_test"'));
  assert.ok(gradle.includes('manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"'));
  const icon = fs.readFileSync(path.join(root, 'app/src/debug/res/mipmap-anydpi/ic_launcher_test.xml'), 'utf8');
  assert.ok(icon.includes('@drawable/ic_launcher_test_foreground'));
  const release = fs.readFileSync(path.join(root, '.github/workflows/release.yml'), 'utf8');
  assert.ok(release.includes(':app:testDebugUnitTest -PciBuild=false'), 'Release must also test the production configuration');
});
