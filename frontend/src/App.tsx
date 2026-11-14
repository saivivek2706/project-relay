import { useEffect, useState } from 'react';
import { getAlert, getAudit, listAlerts, patchStatus } from './api';
import type { AlertDetail, AlertSummary, AuditRow } from './types';
import { AlertList } from './components/AlertList';
import { ChainView } from './components/ChainView';
const STATUSES = ['OPEN','INVESTIGATING','ESCALATED','FALSE_POSITIVE'];
const NEXT: Record<string,string[]> = { OPEN: ['INVESTIGATING'], INVESTIGATING: ['ESCALATED','FALSE_POSITIVE'] };
export default function App(){
  const [status,setStatus]=useState('OPEN'); const [alerts,setAlerts]=useState<AlertSummary[]>([]);
  const [selected,setSelected]=useState<number|undefined>(); const [detail,setDetail]=useState<AlertDetail|null>(null);
  const [audit,setAudit]=useState<AuditRow[]>([]); const [note,setNote]=useState(''); const [actor,setActor]=useState('analyst1'); const [error,setError]=useState('');
  useEffect(()=>{ listAlerts(status).then(d=>setAlerts(d.content)).catch(()=>setAlerts([])); },[status]);
  async function select(id:number){ setSelected(id); const d=await getAlert(id); setDetail(d); setAudit(await getAudit(id)); }
  async function change(to:string){ if(!selected) return; try{ await patchStatus(selected,to,note,actor); setError(''); const d=await getAlert(selected); setDetail(d); setAudit(await getAudit(selected)); setAlerts(await listAlerts(status).then(x=>x.content)); }catch(e:any){ setError(e.message); } }
  return <main style={{fontFamily:'system-ui',maxWidth:1100,margin:'0 auto',padding:16}}>
    <h1>Project Relay — Alerts</h1>
    <label>Status <select value={status} onChange={e=>{setStatus(e.target.value); setDetail(null);}}>{STATUSES.map(s=><option key={s}>{s}</option>)}</select></label>
    <AlertList alerts={alerts} onSelect={select} selectedId={selected}/>
    {detail && <>
      <ChainView alert={detail}/>
      <section><h3>Disposition</h3>{(NEXT[detail.status]||[]).map(to=><button key={to} onClick={()=>change(to)} style={{marginRight:8}}>{to.replace('_',' ')}</button>)}
      <input placeholder="Note" value={note} onChange={e=>setNote(e.target.value)} style={{marginRight:8}}/>
      <input placeholder="Actor" value={actor} onChange={e=>setActor(e.target.value)}/>{error && <p role="alert" style={{color:'red'}}>{error}</p>}</section>
      <section><h3>Audit trail</h3><ul>{audit.map(a=><li key={a.auditId}>{a.at}: {a.fromStatus} → {a.toStatus} by {a.actor}{a.note?` — ${a.note}`:''}</li>)}</ul></section>
    </>}
  </main>;
}
