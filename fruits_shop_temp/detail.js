console.log(fruits)
// 쿼리스트링에서 id 추출
const params = new URLSearchParams(location.search);
const id = Number(params.get("id"));

// 데이터 조회 (과일/채소 모두에서 찾기)
const item = (fruits.find(f=>f.id===id) ?? veggies.find(v=>v.id===id));

const imgEl = document.getElementById("detailImage");
const nameEl = document.getElementById("detailName");
const descEl = document.getElementById("detailDesc");
const priceEl = document.getElementById("detailPrice");
const tabContent = document.getElementById("tabContent");

if (!item){
  nameEl.textContent = "상품을 찾을 수 없습니다.";
} else {
  imgEl.src = item.img;
  imgEl.alt = item.name;
  nameEl.textContent = item.name;
  descEl.textContent = item.content || "신선한 상품으로 준비했습니다.";
  priceEl.textContent = (item.price).toLocaleString() + "원";
}

// 탭 버튼 동작
document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    const idx = btn.dataset.idx;
    tabContent.textContent = `내용${idx}`;
  });
});

// ----- 로드 시 애니메이션: start -> end -----
window.addEventListener("load", () => {
  requestAnimationFrame(()=>{
    document.getElementById("detailImage").classList.add("end");
  });
});
