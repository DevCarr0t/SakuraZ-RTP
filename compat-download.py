from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import urllib.request, json, hashlib, argparse

project=Path(__file__).resolve().parent
default_root=project.parents[1]/'test-servers/RandomTeleportCompatibility' if project.parent.name=='plugin-src' else project/'build/compatibility'
args=argparse.ArgumentParser();args.add_argument('--server-root',type=Path,default=default_root)
root=args.parse_args().server_root.resolve()
root.mkdir(parents=True,exist_ok=True)
UA='RandomTeleportPlugin/1.1 (https://github.com/DevCarr0t/RTP-Plugin)'
VERSIONS=['1.21','1.21.1','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7','1.21.8','1.21.9','1.21.10','1.21.11','26.1.1','26.1.2','26.2']
def fetch(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':UA}),timeout=90)
def download(version):
    folder=root/version;folder.mkdir(exist_ok=True)
    with fetch(f'https://fill.papermc.io/v3/projects/paper/versions/{version}/builds') as r:builds=json.load(r)
    build=next((b for b in builds if b['channel'] in ('STABLE','RECOMMENDED')),builds[0])
    d=build['downloads']['server:default'];file=folder/d['name']
    if not file.exists() or hashlib.sha256(file.read_bytes()).hexdigest()!=d['checksums']['sha256']:
        with fetch(d['url']) as r,file.with_suffix('.partial').open('wb') as f:
            while block:=r.read(1024*1024):f.write(block)
        if hashlib.sha256(file.with_suffix('.partial').read_bytes()).hexdigest()!=d['checksums']['sha256']:raise RuntimeError('Checksum mismatch '+version)
        file.with_suffix('.partial').replace(file)
    (folder/'download.json').write_text(json.dumps(build,indent=2))
    print('Verified Paper '+version+' build '+str(build['id']),flush=True)
    return {'version':version,'build':build['id'],'jar':str(file),'sha256':d['checksums']['sha256']}
with ThreadPoolExecutor(max_workers=3) as pool:
    results=[f.result() for f in as_completed([pool.submit(download,v) for v in VERSIONS])]
(root/'downloads.json').write_text(json.dumps(sorted(results,key=lambda x:VERSIONS.index(x['version'])),indent=2))
