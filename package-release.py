"""Package the verified runtime JAR and source-only ZIP, without test binaries."""
from pathlib import Path
import argparse, hashlib, json, shutil, struct, zipfile

project=Path(__file__).resolve().parent
parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,default=project/'dist')
out=parser.parse_args().output.resolve();out.mkdir(parents=True,exist_ok=True)
jar=project/'dist/RandomTeleportPlugin-1.1.0.jar'
digest=hashlib.sha256(jar.read_bytes()).hexdigest()
with zipfile.ZipFile(jar) as z:
    meta=z.read('plugin.yml').decode()
    assert 'author: SakuraZ' in meta and "api-version: '1.21'" in meta
    assert struct.unpack('>H',z.read('RandomTeleportPlugin.class')[6:8])[0]==65
    assert not any('CompatibilityProbe' in name for name in z.namelist())
    assert z.testzip() is None
results=json.loads((project/'purpur-compatibility-results.json').read_text())
assert len(results)==13 and all(r['status']=='PASS' and r['checks']==32 and r['exit_code']==0 and r['plugin_sha256']==digest for r in results)
target=out/jar.name
if target!=jar.resolve():shutil.copy2(jar,target)
source_zip=out/'RandomTeleportPlugin-1.1.0-src.zip'
count=0
with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED) as z:
    for f in sorted(project.rglob('*')):
        if not f.is_file():continue
        rel=f.relative_to(project)
        if rel.parts[0] in ('.git','dist','test-classes','build','__pycache__') or rel.parts[:2]==('compat-tests','classes'):continue
        if f.suffix in ('.java','.yml','.md','.ps1','.py','.json') or f.name in ('.gitignore','LICENSE'):
            z.write(f,'RandomTeleportPlugin/'+rel.as_posix());count+=1
with zipfile.ZipFile(source_zip) as z:assert z.testzip() is None
for f in (target,source_zip):f.with_suffix(f.suffix+'.sha256').write_text(hashlib.sha256(f.read_bytes()).hexdigest()+'  '+f.name+'\n')
print('Verified JAR:',target)
print('Verified source ZIP:',source_zip)
print('Source files:',count)
print('JAR SHA-256:',digest)
