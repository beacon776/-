<script setup>
import axios from 'axios'
import {nextTick, onUnmounted, reactive, ref} from 'vue'
import * as echarts from 'echarts'

const chartRef = ref(null)
let chartInstance = null

let showHandle = ref(true);
function handleClick() {
  state1.rating = null
  state1.rank = null
  state2.rating = null
  state2.historyData = []
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  showHandle.value = !showHandle.value;
}

const state1 = reactive({
  handle: '', // 输入框绑定的值
  rating: null,
  rank: null,
  error: null,
  isFetching: false,
})

const state2 = reactive({
  handle: '', // 输入框绑定的值
  rating: null,
  history: [],
  isFetching: false,
  error: null
})

// 查handle
const fetchHandleData = async () => {
  console.log('开始查询，当前输入的 handle:', state1.handle)
  if (!state1.handle) {
    alert("请输入 handle")
    console.log('handle为空，停止查询')
    return
  }
  try {
    state1.isFetching = true;
    console.log('正在发送请求...')
    // axios的写法就简便了很多。
    const { data } = await axios.get(`http://127.0.0.1:8080/batchGetUserInfo?handles=${state1.handle}`)
    console.log('请求成功，返回的Handle数据:', data)
    // data是一个长度为1的数组哦
    if (Array.isArray(data)) {
      state1.rating = data[0].rating
      state1.rank = data[0].rank
      console.log('state1:', state1)
    } else {
      console.log('返回数据不是有效的数组，清空历史数据');
    }
  } catch (error) {
    // ?.可选链: 如果不为null或者undefined，就接着访问
    // ||运算: 如果前半部分是null或者undefined，就返回后半部分的值。
    state1.error = error.response?.data?.message || '请求失败';
    console.error('请求发生错误:', error)
  }

}
// 查rating
const fetchRatingData = async () => {
  console.log('开始查询，当前输入的 handle:', state2.handle)
  if (!state2.handle) {
    alert("请输入 handle")
    return
  }

  try {
    state2.isFetching = true;
    // 用axios获取数据 异步函数中await等到axios执行完毕后，再进行下面的操作
    const { data } = await axios.get(`http://127.0.0.1:8080/getUserRatings?handle=${state2.handle}`)
    //
    if (Array.isArray(data)) {
      // 把字符串yyyy-mm-dd hh-mm-ss先转成date，再转成秒，便于比较（喵的我数据库返回的数据的日期莫名其妙的乱了，得手动排序）
      state2.historyData = data.sort((a, b) =>
      new Date(a.ratingUpdatedAt.replace(" ", "T")).getTime() - new Date(b.ratingUpdatedAt.replace(" ", "T")).getTime())
      state2.rating = data[data.length - 1].newRating
      // 等待下次dom更新
      await nextTick()
      // 初始化图表
      initChart()
    }
  } catch (error) {
    state2.error = error.response?.data?.message || '请求失败'
    console.error('请求发生错误:', error)
  } finally {
    state2.isFetching = false
  }
}

// 图表初始化函数
const initChart = () => {
  if (!chartRef.value || !state2.historyData) return


  const seriesData = state2.historyData.map(item => ({
    name: item.ratingUpdatedAt,
    value: item.newRating,
    rank: item.rank,
    contestName: item.contestName,
    change: item.newRating - item.oldRating,
  }))

  const option = {
    grid: {
      left: '25%',   // 控制左边距
      right: '30%',  // 控制右边距
    },
    xAxis: {
      type: 'category', // 分散数据
      data: state2.historyData.map(item => item.ratingUpdatedAt), // 只显示比赛时间
      name: '比赛时间',
    },
    yAxis: {
      type: 'value',
      name: 'Rating',
    },
    series: [{
      data: seriesData,  // !!!!!传递完整数据对象
      type: 'line', // 折线图（直线连接，故smooth属性为false）
    }],
    tooltip: {
      trigger: 'item', // 鼠标悬浮在数据上除法提示框
      extraCssText: 'background-color: rgba(255, 240, 240, 0.1); border-radius: 0; width: auto; height: 100px; padding: 3px;',
      formatter: function (params) {
        const time = params.name
        const rating = params.value
        const rank = params.data.rank
        const change = params.data.change
        const formattedChange = change > 0 ? `+${change}` : change; // 为正数的 change 添加 "+"，负数和零保持不变
        const contestName = params.data.contestName
        // 格式化返回要显示的内容，换行显示
        return `
        <div>= ${rating} (${formattedChange})</div>
        <div>Rank: ${rank}</div>
        <div>${contestName}</div>
        <div>${time}</div>
      `;
      }
    }
  }
  // 如果当前已经有图标的话，需要先销毁当前图表
  if (chartInstance) {
    chartInstance.dispose()
  }

  // 初始化ECharts实例
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(option) // 用这个Echarts实例设置图表选项
}

// 组件销毁时清理（这里是组件销毁时的那个钩子函数）
onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>

<template>
  <div class="container">
    <header>
      <div class="title">
        查询选手
      </div>
      <KeepAlive>
      <div class="switch">
          <button @click="handleClick">
            切换表单
            <!--(先查询handle，后查询rating)-->
          </button>
      </div>
      </KeepAlive>
      <div class="query-button">
        <div>
         <!---model双向绑定-->
          <input type="text" placeholder="输入 handle" v-model="state1.handle" v-if="showHandle">
          <input type="text" placeholder="输入 handle" v-model="state2.handle" v-else>
        </div>
        <!-- 先调用fetchHandle的方法-->
        <div class="query" @click="fetchHandleData" v-if="showHandle">
          <input type="button" value="查询" id="query-button-small">
        </div>
         <!--后调用fetchRating的方法-->
        <div class="query" @click="fetchRatingData" v-else>
          <input type="button" value="查询" id="query-button-small">
        </div>
      </div>
    </header>

    <div class="information" id="result">
      <div class="title">
        查询结果
      </div>
      <div class="handleResult" v-if="!showHandle">
        <div id="handleResult" >handle: {{ state2.handle }}</div>
        <div id="ratingResult">rating: {{ state2.rating }}</div>
       <!--！！！！注意这里state.historyData.length该是个固定的常值，不该是用插值表达式引起来的变量，要不然就错了。
        所以我在下面提前处理好了state2.rating这个值。-->
      </div>
      <div class="answer">
        <div v-if="showHandle" class="handleResult">
          <p id="text1">handle:{{state1.handle}}</p>
          <p>rating:{{state1.rating}}</p>
          <p>rank:{{state1.rank}}</p>
        </div>
        <div v-else class="ratingResult">
          <div ref="chartRef" style="width: 100%; height: 550px">
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
header {
  position: relative;
  width: 100%;
  height: 30%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 0 40px 0;
  gap: 40px;
}
#result {
  position: relative;
  width: 100%;
  height: 65vh;
  flex-direction: column;
  align-items: center;
  display: flex;
}

.title {
  font-size: 25px;
}
.query {
  display: flex;
  justify-content: center;
  align-items: center;
}
.query-button {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 10px;
  width: 100%;
}
input {
  width: 100%;
  height: 30px;
}
#query-button-small {
  width: 40px;
  height: 30px;
}
#ratingHistory {
  width: 80%;
  margin-top: 20px;
  border-collapse: collapse;
}
#ratingHistory th, #ratingHistory td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: center;
}
.answer {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  overflow-y: auto; /* 内容溢出时加个滚动条 */
  width: 100%;
  justify-content: flex-start;
  gap: 40px;
}
#ratingHistory {
  width: 80%;
  margin-top: 20px;
  border-collapse: collapse;
  max-height: 50vh;
  overflow-y: auto;
  justify-content: flex-start;
  padding-left: 15vw;
}
#ratingHistory td {
  height: 30px;
  text-align: center;
  padding: 5px;
}
.handleResult {
  position: sticky;
  display: flex;
  flex-direction: column;
  gap: 25px;
  width: 100%;
  justify-content: flex-start;
  align-content: center;
  padding: 0 0 0 23vw;
  margin-bottom: 20px;
}
.handleResult p {
  font-size: 18px;
}
#text1 {
  margin-top: 4vh;
}
#handleResult {
  margin: 33px 0 0 0;
}
.ratingResult {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
