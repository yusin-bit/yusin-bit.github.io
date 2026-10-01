let mockData = [
{id:0, isDone:false, content:"React study", date: new Date().getTime()},
{id:1, isDone:true, content:"친구만나기", date: new Date().getTime()},
{id:2, isDone:false, content:"낮잠자기", date: new Date().getTime()},
];

let day =["일","월","화","수","목","금","토"];


//////////////////////////////초기 값/////////////////////////////////////
onload = () => {
    initData(mockData);
    
    const now = new Date();

    const year = now.getFullYear();
    const Month = now.getMonth() +1;
    const DateNow = now.getDate();
    const dayNum = now.getDay();
    const toDay = day[dayNum];


     document.querySelector("#date").innerText = `${year}년 ${Month}월 ${DateNow}일 ${toDay}요일`;

}
////////////////////////////////현재 날짜 생성////////////////////////////////////////
let idIndex = 3;

document.querySelector(".Editor > button").addEventListener("click", (event) => {
    event.preventDefault();


     if(document.querySelector(".Editor > input").value.trim() === "") {
            alert("할 일을 입력해 주세요");
            return;
        }

    
    const newTodo = {
        id:idIndex++,
        isDone: false,
        content: document.querySelector(".Editor > input").value,
        date: new Date().getTime()

        
    }

   
    
    mockData.push(newTodo);
    initData(mockData);
})

//////////////////////////////사용자 입력에 따른 객체 생성//////////////////////////////

const initData = (printData) => {
    document.querySelector(".todos_wrapper").innerHTML=""
    printData.forEach((todo) => {
        document.querySelector(".todos_wrapper").innerHTML +=
        `<div class = "TodoItem">
                <input type = "checkbox" onchange = "onUpdate(${todo.id})" ${todo.isDone ? "checked" : ""}>
                <div class = "content">${todo.content}</div>
                <div class = "date">${new Date(todo.date).toLocaleString()}</div>
                <button name="todoDel"
                    value="${todo.id}" onclick="todoDel(this)">삭제</button>
        </div>`

       
     

})}

///////////////////////////입력된 값이 저장된 배열의 값을 추가///////////////////////////////

const onUpdate = (targetId) => {
    mockData.map((todo) => {
        if(todo.id === targetId) {
            todo.isDone = !todo.isDone;
        }

        return todo;
    })

    initData(mockData);
}
//////////////////////////수정 기능////////////////////////////////////////////////////////

const todoDel = (th) => {

    const targetId = Number(th.value);

    mockData = mockData.filter((todo) => {
        return todo.id !== targetId;
    });

    initData(mockData);
}

document.querySelector("#keyword").addEventListener("keyup", () => {
    let searchTodos = getFilterData(event.target.value);

    initData(searchTodos);

});

const getFilterData = (search) =>{
 //검색어가 없으면 mockData를 리턴한다.
 if(search===""){
 return mockData;
 }

 let filteredData = mockData.filter((todo) => {
        return todo.content.includes(search);
    });

    return filteredData;

}
