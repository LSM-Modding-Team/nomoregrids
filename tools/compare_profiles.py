#!/usr/bin/env python3
"""Read-only F9 comparison. Never starts the game, tests or builds; never certifies all of F9."""
import argparse,json,math,re
from pathlib import Path

COUNTS={'wall10000':10000,'dense4096':4096,'pb256':256,'boundary8':512}
def require(condition,message):
    if not condition:raise ValueError(message)
def finite(value):return isinstance(value,(int,float)) and not isinstance(value,bool) and math.isfinite(value)
def window(data,min_samples):
    require(data.get('complete') is True,'Incomplete measurement window')
    require(isinstance(data.get('samples'),int) and data['samples']==data.get('target') and data['samples']>=min_samples,'Sample count/target')
    require(data.get('warmupSamples',-1)>=100,'Warmup missing')
    for key in ['p50Nanos','p95Nanos','p99Nanos','meanNanos']:require(finite(data.get(key)) and data[key]>=0,'Invalid '+key)
    require(data['p50Nanos']<=data['p95Nanos']<=data['p99Nanos'],'Unordered percentiles')
def hardware(a,b):
    require(a and a==b,'Hardware, JVM, quota, heap or revision differs')
    require(a.get('hardwareTag'),'Operator hardware tag is required')
    require(re.fullmatch(r'(?:[0-9a-f]{40}|[0-9a-f]{64})',a.get('revisionTag','')) is not None,'Full revision tag is required')
def interval(data):
    a,b=data.get('startedEpochMillis'),data.get('finishedEpochMillis')
    require(finite(a) and finite(b) and 0<a<b,'Measurement timestamps missing');return a,b

def compare(base,scene,base_client,scene_client,loads):
    name=scene.get('scene');require(name in COUNTS,'Not a supported scene')
    require(base.get('scene')=='native_'+name,'Use the equivalent native fixture, not an empty-world baseline')
    hardware(base.get('hardware'),scene.get('hardware'))
    for a in [base,scene]:
        require(a.get('schema')==1 and a.get('phase')=='COMPLETE' and not a.get('error'),'Failed/incompatible server report')
        window(a.get('tickWindow',{}),1200);interval(a);require(a.get('fixturesVerified') is True,'Fixtures not verified after measurement')
        require(a.get('fixtures')==COUNTS[name],'Wrong fixture count')
    require(scene.get('authoritativePieces')==COUNTS[name] and base.get('authoritativePieces')==0,'Scene authority count')
    require(base.get('players')==scene.get('players'),'Player count differs')
    for key in ['profileHash','minecraft','neoforge','anchor','mods']:require(key in scene and base.get(key)==scene[key],key+' differs or is missing')
    require(base['tickWindow']['warmupSamples']==scene['tickWindow']['warmupSamples'],'Warmup differs')
    result={'schema':1,'scene':name,'comparableServer':True,'serverP95Ms':scene['tickWindow']['p95Nanos']/1e6,'serverThresholdPass':scene['tickWindow']['p95Nanos']<=50e6,'clientComparison':None,'fourActorLoad':None,'f9Certified':False}
    if base_client is not None or scene_client is not None:
        require(base_client is not None and scene_client is not None,'Provide both client reports')
        hardware(base_client.get('hardware'),scene_client.get('hardware'))
        require(base_client['hardware']['revisionTag']==base['hardware']['revisionTag'],'Client/server revisions differ')
        for server,client in [(base,base_client),(scene,scene_client)]:
            require(client.get('schema')==1 and client.get('kind')=='client-frame-window' and client.get('aborted') is False,'Aborted/incompatible client report')
            require(client.get('label')==server.get('run'),'Client label must be its server run UUID')
            require(client.get('profileHash')==server.get('profileHash'),'Client physical profile differs')
            window(client.get('frameInterval',{}),600);window(client.get('cpuRender',{}),600)
            sa,sb=interval(server);ca,cb=interval(client);require(sa-1000<=ca<cb<=sb+1000,'Client window is outside its server measurement (synchronize clocks)')
        for key in ['gpu','graphicsBackend','gpuDriver','options','yaw','pitch','mods']:require(key in base_client and base_client[key]==scene_client.get(key),key+' differs')
        offset=.5
        a,b=base_client.get('camera'),scene_client.get('camera');require(isinstance(a,list) and isinstance(b,list) and len(a)==len(b)==3,'Camera metadata')
        require(all(finite(x) for x in a+b),'Camera values')
        require(abs(b[0]-a[0]-offset)<.011 and abs(b[1]-a[1])<.011 and abs(b[2]-a[2])<.011,'Camera must compensate the scene offset')
        delta=(scene_client['frameInterval']['p95Nanos']-base_client['frameInterval']['p95Nanos'])/1e6
        result['clientComparison']={'frameIntervalP95DeltaMs':delta,'thresholdPass':delta<=5,'gpuTimeMeasured':False}
    if loads:
        require(len(loads)==4 and scene.get('players')==4,'Four distinct connected clients are required')
        ids=set();starts=[];ends=[]
        for load in loads:
            require(load.get('schema')==1 and load.get('kind')=='client-intent-load' and load.get('aborted') is False,'Invalid load report')
            require(load.get('label')==scene.get('run') and load.get('mutatingActionsSent') is False,'Load run identity/actions')
            require(load.get('hardware',{}).get('revisionTag')==scene['hardware']['revisionTag'] and load.get('hardware',{}).get('hardwareTag')==scene['hardware']['hardwareTag'],'Load revision/hardware label differs')
            require(load.get('acceptedCurrent',0)>0 and load.get('rejectedStale',0)>0 and load.get('acceptedStale')==0,'Missing positive/negative outcomes or stale request accepted')
            require(load.get('connectionRunId'),'Missing connection identity');ids.add(load['connectionRunId']);interval(load);a,b=load.get('startedSendingEpochMillis'),load.get('finishedSendingEpochMillis');require(finite(a) and finite(b) and 0<a<b,'Active sending timestamps missing');starts.append(a);ends.append(b)
        require(len(ids)==4,'Reports reuse a client connection')
        sa,sb=interval(scene);overlap=min(ends+[sb])-max(starts+[sa]);require(overlap>=50000,'Four-client overlap must cover at least 50 seconds')
        result['fourActorLoad']={'overlapMillis':overlap,'acceptedStale':0,'rateLimitDropsMeasured':False}
    result['pairedFrameComparisonComplete']=result['clientComparison'] is not None
    return result

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('baseline',type=Path);p.add_argument('scene',type=Path);p.add_argument('--baseline-client',type=Path);p.add_argument('--scene-client',type=Path);p.add_argument('--load',action='append',type=Path,default=[]);args=p.parse_args()
    def read(path):
        if path is None:return None
        require(path.stat().st_size<=4*1024*1024,'Report size limit');return json.loads(path.read_text())
    try:result=compare(read(args.baseline),read(args.scene),read(args.baseline_client),read(args.scene_client),[read(x) for x in args.load])
    except (ValueError,KeyError,TypeError,OSError) as ex:print(json.dumps({'comparable':False,'reason':str(ex),'f9Certified':False},ensure_ascii=False));return 2
    print(json.dumps(result,ensure_ascii=False,indent=2));return 0
if __name__=='__main__':raise SystemExit(main())
