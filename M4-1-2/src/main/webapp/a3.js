const queryButton = document.getElementById('query-button-small');
const input = document.getElementById('input');
const resultDiv = document.getElementById('result');
const handleResult = document.getElementById('handleResult');
const ratingResult = document.getElementById('ratingResult');
const ratingHistoryTable = document.getElementById('ratingHistory').getElementsByTagName('tbody')[0];

// 给查询按钮添加事件监听
queryButton.addEventListener('click', function () {
    console.log('Query Button Clicked');
    // 获取用户名并去掉两边的空格
    const username = input.value.trim();
    if (!username) {
        alert('请输入用户名！');
        return;
    }

    // 创建一个新的XMLHttpRequest对象用于发起AJAX请求
    const xhr = new XMLHttpRequest();
    xhr.open('GET', `http://127.0.0.1:2333/getUserRatings?handle=${encodeURIComponent(username)}`);
    // 当请求状态发生变化时调用该回调函数
    xhr.onreadystatechange = function () {
        // 这里的readyState: 2已接收，3请求处理中，4请求完成，响应已准备好。它是动态变化的
        console.log('ReadyState:', xhr.readyState);
        console.log('Status:', xhr.status);

        // 请求完成时
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                console.log('Raw Response:', xhr.responseText);
                try {
                    // 请求成功，处理返回数据
                    const data = JSON.parse(xhr.responseText);
                    console.log('Parsed Data:', data);

                    // 显示第一栏处理结果（我们取data数组的最后一组数据作为当前handle的数据，我懒得写查询第一个接口了......
                    handleResult.textContent = `Handle: ${data[data.length - 1].handle}`;
                    ratingResult.textContent = `Rating: ${data[data.length - 1].newRating}`;

                    console.log('Handle:', handleResult.textContent);
                    console.log('Rating:', ratingResult.textContent);

                    // 清空表格
                    ratingHistoryTable.innerHTML = ``;
                    console.log('data.history:', data.history);

                    // 有数据并且rank不为undefined（说明打过比赛）
                    if (data.length > 0 && data[0].rank !== undefined) {
                        data.forEach(item => {
                            const row = ratingHistoryTable.insertRow();
                            row.insertCell(0).textContent = item.ratingUpdatedAt;
                            row.insertCell(1).textContent = item.contestName;
                            row.insertCell(2).textContent = item.rank;
                            row.insertCell(3).textContent = `${item.oldRating} -> ${item.newRating}`;
                            console.log('column1:', item.ratingUpdatedAt);
                            console.log('column2:', item.contestName);
                            console.log('column3:', item.rank);
                            console.log('column4:', item.oldRating , item.newRating);
                        });
                    } else {
                        // 没有数据或者根本没打过比赛
                        const row = ratingHistoryTable.insertRow();
                        const cell = row.insertCell(0);
                        console.log('该用户没有历史记录')
                        cell.colSpan = 4; // 当前单元格跨越四列占据整个表格
                        cell.textContent = '没有历史记录'
                    }
                } catch (e) {
                    // 解析数据时发生错误
                    console.error('Parse Error:', e);
                    handleError({ message: "响应数据解析失败"});
                }
            } else {
                // 状态码不是200
                handleError({ message: `HTTP错误 ${xhr.status}`  });
            }
        }
    };
    // 请求发生错误
    xhr.onerror = function (e) {
        console.error('Network Error:', e);
        handleError({message: '网络连接异常' })
        resultDiv.classList.add('visible'); // 显示表单
    };
    // 发送请求
    xhr.send();
});

function handleError(error) {
    console.log('Error:', error);
    ratingHistoryTable.innerHTML =
        `<tr><td colspan="4">Error: ${error.message || 'Unknown error'}</td></tr>`;
    handleResult.textContent = 'Handle: ';
    ratingResult.textContent = 'Rating: ';
}

// 按键盘的Enter键也顺手监听一下，自动触发鼠标点击事件
input.addEventListener('keypress',  (e) => {
    if (e.key  === 'Enter') queryButton.click()
})