"""Regression test using an installed Minecraft/Forge library set and SGJourney jar."""
import argparse
import json
import subprocess
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--install', type=Path, required=True)
p.add_argument('--sgjourney', type=Path, required=True)
p.add_argument('--java-home', type=Path, required=True)
p.add_argument('--forge', default='47.4.16')
a = p.parse_args()
root = Path(__file__).resolve().parents[1]
out = root / 'build/route-tests'
out.mkdir(parents=True, exist_ok=True)
libs = a.install / 'libraries'
cp = [out, a.sgjourney, libs / 'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar']
cp.append(libs / 'net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-extra.jar')
for version in ['1.20.1', 'forge-' + a.forge]:
    data = json.loads((a.install / 'versions' / version / (version + '.json')).read_text())
    for lib in data['libraries']:
        artifact = (lib.get('downloads') or {}).get('artifact')
        if artifact and (libs / artifact['path']).exists():
            cp.append(libs / artifact['path'])
cp.extend(libs.glob('net/minecraftforge/forge/1.20.1-' + a.forge + '/*universal.jar'))
for module in ['fmlcore', 'javafmllanguage', 'lowcodelanguage', 'mclanguage']:
    cp.extend(libs.glob('net/minecraftforge/' + module + '/1.20.1-' + a.forge + '/*.jar'))
classpath = ';'.join(str(x.resolve()).replace('\\','/') for x in cp)
sources = [root/'src/main/java/com/dankscripts/sgjdestiny_dhd/compat/DestinyRouteRepair.java',
           root/'tools/tests/DestinyRouteRepairTest.java']
compile_args = out / 'compile.args'
compile_args.write_text('-proc:none\n-cp\n"' + classpath + '"\n-d\n"' + out.as_posix() + '"\n' +
                        '\n'.join('"'+s.as_posix()+'"' for s in sources))
subprocess.run([str(a.java_home/'bin/javac.exe'), '@'+str(compile_args)], check=True, cwd=root)
run_args = out / 'run.args'
run_args.write_text('-cp\n"' + classpath + '"\ncom.dankscripts.sgjdestiny_dhd.compat.DestinyRouteRepairTest\n')
subprocess.run([str(a.java_home/'bin/java.exe'), '@'+str(run_args)], check=True, cwd=root)
