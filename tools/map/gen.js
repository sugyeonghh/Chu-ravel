// 사용법: cd tools/map && npm install && npm run gen
// 입력: muni.json (southkorea-maps, KOSTAT 2013 시군구 topojson)
// 출력: ../../frontend/public/data/{regions,provs}.json, ../../src/main/resources/regions.csv
const path_=require("path"),fs=require("fs");
const OUT_STATIC=path_.join(__dirname,"../../frontend/public/data");
const OUT_RES=path_.join(__dirname,"../../src/main/resources");
const t=require("./muni.json"),tc=require("topojson-client"),d3=require("d3-geo");
const o=t.objects.skorea_municipalities_geo;
const P={11:"서울",21:"부산",22:"대구",23:"인천",24:"광주",25:"대전",26:"울산",29:"세종",31:"경기",32:"강원",33:"충북",34:"충남",35:"전북",36:"전남",37:"경북",38:"경남",39:"제주"};
const groups={};
for(const g of o.geometries){let c=g.properties.code,k=c.slice(0,4);
 let n=g.properties.name;
 if(n==="청원군"){ // merged into 청주시 in 2014
   const cj=o.geometries.find(x=>x.properties.name.startsWith("청주시")); k=cj.properties.code.slice(0,4);}
 (groups[k]=groups[k]||{geoms:[],names:[]}).geoms.push(g);groups[k].names.push(n);}
const feats=[];
for(const [k,v] of Object.entries(groups)){
 let n=v.names.find(x=>x!=="청원군")||v.names[0];
 if(v.geoms.length>1||/시.+구$/.test(n)){const m=n.match(/^(.+?시)/);if(m)n=m[1];}
 feats.push({type:"Feature",properties:{id:k,name:n,prov:P[k.slice(0,2)]},geometry:tc.merge(t,v.geoms)});}
const fc={type:"FeatureCollection",features:feats};
const DOKDO=[[131.8644,37.2423],[131.8696,37.2414]];
const fitFc={type:"FeatureCollection",features:[...feats,{type:"Feature",geometry:{type:"MultiPoint",coordinates:DOKDO.map(([x,y])=>[x+0.12,y])}}]};
const W=760,H=1000,proj=d3.geoMercator().fitExtent([[10,10],[W-10,H-10]],fitFc);
const path=d3.geoPath(proj).digits(1);
const out=feats.map(f=>{const b=path.bounds(f),c=path.centroid(f);return{id:f.properties.id,n:f.properties.name,p:f.properties.prov,d:path(f),b:b.map(x=>x.map(v=>+v.toFixed(1))),c:c.map(v=>+v.toFixed(1)),a:+path.area(f).toFixed(0)}});

// 독도: 실제 크기로는 보이지 않아 서도·동도를 과장해서 그린다
const blob=(cx,cy,r,seed)=>{let d="";for(let i=0;i<10;i++){const a=i/10*Math.PI*2,rr=r*(0.82+0.18*Math.sin(i*2.3+seed));d+=(i?"L":"M")+(cx+Math.cos(a)*rr).toFixed(1)+","+(cy+Math.sin(a)*rr).toFixed(1);}return d+"Z";};
const [ws,es]=DOKDO.map(p=>proj(p));const mid=[(ws[0]+es[0])/2,(ws[1]+es[1])/2];
const wx=mid[0]-4.2,ex=mid[0]+4.2,yy=mid[1];
const dd=blob(wx,yy-0.6,4.4,1)+blob(ex,yy+0.8,3.6,2);
out.push({id:"DOKDO",n:"독도",p:"경북",d:dd,b:[[+(wx-4.6).toFixed(1),+(yy-5.2).toFixed(1)],[+(ex+3.8).toFixed(1),+(yy+4.6).toFixed(1)]],c:[+mid[0].toFixed(1),+(yy-9).toFixed(1)],a:60});
fs.writeFileSync(path_.join(OUT_STATIC,"regions.json"),JSON.stringify(out));
fs.writeFileSync(path_.join(OUT_RES,"regions.csv"),"code,name,province\n"+out.map(r=>`${r.id},${r.n},${r.p}`).join("\n")+"\n");
console.log("regions",out.length);
const byProv={};for(const g of o.geometries){const p=P[g.properties.code.slice(0,2)];(byProv[p]=byProv[p]||[]).push(g);}
const provs=Object.entries(byProv).map(([p,gs])=>({p,d:path(tc.merge(t,gs))}));
fs.writeFileSync(path_.join(OUT_STATIC,"provs.json"),JSON.stringify(provs));
console.log("provs",provs.length);
