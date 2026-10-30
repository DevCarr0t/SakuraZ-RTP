from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import urllib.request, json, hashlib, argparse

project=Path(__file__).resolve().parent
default_root=project.parents[1]/'test-servers/RandomTeleportPurpurCompatibility' if project.parent.name=='plugin-src' else project/'build/purpur-compatibility'
parser=argparse.ArgumentParser();parser.add_argument('--server-root',type=Path,default=default_root)
root=parser.parse_args().server_root.resolve();root.mkdir(parents=True,exist_ok=True)
UA='RandomTeleportPlugin/1.1 (https://github.com/DevCarr0t/RTP-Plugin)'
def fetch(url):return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':UA}),timeout=90)
with fetch('https://api.purpurmc.org/v2/purpur') as response:published=json.load(response)['versions']
wanted=['1.21','1.21.1','1.21.3','1.21.4','1.21.5','1.21.6','1.21.7','1.21.8','1.21.9','1.21.10','1.21.11','26.1.1','26.1.2','26.2']
versions=[v for v in wanted if v in published]
def download(version):
    folder=root/version;folder.mkdir(exist_ok=True)
    with fetch(f'https://api.purpurmc.org/v2/purpur/{version}/latest') as response:meta=json.load(response)
    assert meta['result']=='SUCCESS'
    build=int(meta['build']);url=f'https://api.purpurmc.org/v2/purpur/{version}/{build}/download'
    target=folder/f'purpur-{version}-{build}.jar'
    if not target.exists() or hashlib.md5(target.read_bytes()).hexdigest()!=meta['md5']:
        partial=target.with_suffix('.partial')
        with fetch(url) as response,partial.open('wb') as stream:
            while block:=response.read(1024*1024):stream.write(block)
        assert hashlib.md5(partial.read_bytes()).hexdigest()==meta['md5'],'Download integrity failed'
        partial.replace(target)
    result={'platform':'purpur','version':version,'build':build,'jar':str(target),'md5':meta['md5'],'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'url':url}
    (folder/'download.json').write_text(json.dumps(result,indent=2))
    print('Verified Purpur '+version+' build '+str(build),flush=True);return result
with ThreadPoolExecutor(max_workers=3) as pool:results=[f.result() for f in as_completed([pool.submit(download,v) for v in versions])]
(root/'downloads.json').write_text(json.dumps(sorted(results,key=lambda x:versions.index(x['version'])),indent=2))
print('Published versions in range: '+str(len(results)),flush=True)
