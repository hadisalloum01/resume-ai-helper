import {useCallback,useEffect,useState} from 'react';
import {Upload,FileText,Sparkles,Clock,Trash2,CheckCircle2,AlertCircle,Lightbulb,Target,Download,BarChart3,Briefcase,Building2} from 'lucide-react';

const API='http://localhost:8080/api';
const EMPTY_STATS={totalAnalyses:0,averageMatchScore:0,highestMatchScore:0,mostCommonMissingSkills:[]};

export default function App(){
 const[file,setFile]=useState(null);
 const[jobTitle,setJobTitle]=useState('');
 const[companyName,setCompanyName]=useState('');
 const[job,setJob]=useState('');
 const[result,setResult]=useState(null);
 const[history,setHistory]=useState([]);
 const[stats,setStats]=useState(EMPTY_STATS);
 const[loading,setLoading]=useState(false);
 const[error,setError]=useState('');

 const load=useCallback(async()=>{
  try{
   const [historyResponse,statsResponse]=await Promise.all([
    fetch(`${API}/analyses`),
    fetch(`${API}/analyses/stats`)
   ]);
   if(historyResponse.ok)setHistory(await historyResponse.json());
   if(statsResponse.ok)setStats(await statsResponse.json());
  }catch{
   // Keep the interface usable if the backend is temporarily unavailable.
  }
 },[]);

 useEffect(()=>{load();},[load]);

 async function analyze(e){
  e.preventDefault();
  setError('');
  if(!file)return setError('Please select a resume.');
  if(!jobTitle.trim())return setError('Please enter a job title.');
  if(!companyName.trim())return setError('Please enter a company name.');
  if(!job.trim())return setError('Please enter a job description.');

  setLoading(true);
  const fd=new FormData();
  fd.append('resume',file);
  fd.append('jobTitle',jobTitle);
  fd.append('companyName',companyName);
  fd.append('jobDescription',job);

  try{
   const response=await fetch(`${API}/analyses`,{method:'POST',body:fd});
   const data=await response.json();
   if(!response.ok)throw new Error(data.message||'Analysis failed.');
   setResult(data);
   await load();
  }catch(exception){
   setError(exception.message);
  }finally{
   setLoading(false);
  }
 }

 async function remove(id){
  if(!confirm('Delete this analysis?'))return;
  const response=await fetch(`${API}/analyses/${id}`,{method:'DELETE'});
  if(!response.ok){setError('Could not delete the analysis.');return;}
  if(result?.id===id)setResult(null);
  await load();
 }

 function downloadPdf(id){
  window.open(`${API}/analyses/${id}/pdf`,'_blank','noopener,noreferrer');
 }

 return <div className="app">
  <header><div className="brand"><div className="logo"><Sparkles/></div><div><h1>Resume AI Helper</h1><p>Turn your resume into a stronger application</p></div></div><span className="badge">Portfolio Project</span></header>
  <main>
   <section className="hero"><div><span className="eyebrow">SMARTER JOB APPLICATIONS</span><h2>See how well your resume matches the role.</h2><p>Upload your resume, enter the role details, and receive practical recommendations in seconds.</p></div><div className="hero-stat"><Target/><strong>ATS-ready insights</strong><span>Focused, transparent, useful</span></div></section>

   <Stats stats={stats}/>

   <div className="grid">
    <form className="panel form" onSubmit={analyze}>
     <h3>New analysis</h3>
     <label className={`drop ${file?'selected':''}`}><input type="file" accept=".pdf,.docx,.txt" onChange={e=>setFile(e.target.files[0]||null)}/><Upload/><strong>{file?file.name:'Upload your resume'}</strong><span>PDF, DOCX, or TXT · max 10 MB</span></label>
     <div className="field-row">
      <label><span><Briefcase size={15}/>Job title</span><input type="text" placeholder="Software Engineer" value={jobTitle} onChange={e=>setJobTitle(e.target.value)}/></label>
      <label><span><Building2 size={15}/>Company</span><input type="text" placeholder="Example Company" value={companyName} onChange={e=>setCompanyName(e.target.value)}/></label>
     </div>
     <label>Job description<textarea rows="11" placeholder="Paste the job description here..." value={job} onChange={e=>setJob(e.target.value)}/></label>
     {error&&<div className="error"><AlertCircle size={18}/>{error}</div>}
     <button disabled={loading}>{loading?<><span className="spinner"/>Analyzing…</>:<><Sparkles size={18}/>Analyze resume</>}</button>
    </form>

    <section className="panel result">{result?<Results data={result} onDownload={downloadPdf}/>:<div className="empty"><FileText/><h3>Your report will appear here</h3><p>We will show your match score, strengths, missing skills, recommendations, and a downloadable PDF report.</p></div>}</section>
   </div>

   <section className="history">
    <div className="section-title"><div><h3>Analysis history</h3><p>Your latest saved reports</p></div><Clock/></div>
    {history.length===0?<div className="history-empty">No saved analyses yet.</div>:<div className="cards">{history.map(x=><article key={x.id} className="history-card" onClick={()=>setResult(x)}><div className="score small">{x.matchScore}%</div><div><strong>{x.jobTitle}</strong><span>{x.companyName} · {x.fileName}</span><span>{formatDate(x.createdAt)} · {x.aiGenerated?'AI analysis':'Local analysis'}</span></div><div className="card-actions"><button title="Download PDF" className="icon" onClick={e=>{e.stopPropagation();downloadPdf(x.id)}}><Download size={17}/></button><button title="Delete" className="icon delete" onClick={e=>{e.stopPropagation();remove(x.id)}}><Trash2 size={17}/></button></div></article>)}</div>}
   </section>
  </main>
  <footer>Built with Spring Boot, React, PostgreSQL, PDFBox, and OpenAI.</footer>
 </div>
}

function Stats({stats}){
 const skills=stats.mostCommonMissingSkills||[];
 return <section className="stats-grid">
  <article className="stat-card"><BarChart3/><span>Total analyses</span><strong>{stats.totalAnalyses}</strong></article>
  <article className="stat-card"><Target/><span>Average score</span><strong>{Number(stats.averageMatchScore||0).toFixed(1)}%</strong></article>
  <article className="stat-card"><Sparkles/><span>Highest score</span><strong>{stats.highestMatchScore||0}%</strong></article>
  <article className="stat-card wide"><AlertCircle/><span>Common missing skills</span><strong className="skill-list">{skills.length?skills.join(', '):'No data yet'}</strong></article>
 </section>
}

function Results({data,onDownload}){
 return <div>
  <div className="result-head"><div><span className="eyebrow">MATCH REPORT</span><h3>{data.jobTitle}</h3><p>{data.companyName} · {data.fileName}</p><p>{data.aiGenerated?'Generated with AI':'Generated with local analysis'}</p></div><div className="score">{data.matchScore}%</div></div>
  <button className="download-button" onClick={()=>onDownload(data.id)}><Download size={18}/>Download PDF report</button>
  <Block icon={<CheckCircle2/>} title="Strengths" items={data.strengths}/>
  <Block icon={<AlertCircle/>} title="Missing skills" items={data.missingSkills}/>
  <Block icon={<Lightbulb/>} title="Recommendations" items={data.recommendations}/>
  <div className="summary"><h4>Improved professional summary</h4><p>{data.improvedSummary}</p></div>
 </div>
}

function Block({icon,title,items}){
 return <div className="block"><h4>{icon}{title}</h4><ul>{items?.length?items.map((x,i)=><li key={i}>{x}</li>):<li>None identified.</li>}</ul></div>
}

function formatDate(value){
 if(!value)return 'Unknown date';
 const date=new Date(value);
 return Number.isNaN(date.getTime())?'Unknown date':date.toLocaleString();
}
