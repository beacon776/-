<script setup>
import axios from 'axios'
import { reactive, ref } from 'vue'

let showHandle = ref(true);
function handleClick() {
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
      state1.rating.value = 0
      state1.rank.value = 0
    }
  } catch (error) {
    state1.rating.value = 0
    state1.rank.value = 0
    state1.error = error.response?.data?.message || '请求失败';
    console.error('请求发生错误:', error)
  }

}

// 查rating
const fetchRatingData = async () => {
  console.log('开始查询，当前输入的 handle:', state2.handle)
  if (!state2.handle) {
    alert("请输入 handle")
    console.log('handle为空，停止查询')
    return
  }
  try {
    state2.isFetching = true;
    console.log('正在发送请求...')
    const { data } = await axios.get(`http://127.0.0.1:8080/getUserRatings?handle=${state2.handle}`)
    console.log('请求成功，返回的Rating数据:', data)

    // 确保返回的数据是有效的数组
    if (Array.isArray(data)) {
      state2.historyData = data; // 将返回的数据存入 historyData
      console.log('historyData:', state2.historyData)
      console.log('length:', state2.historyData.length)
      state2.rating = data[state2.historyData.length - 1].newRating
      console.log('state2:', state2)
    } else {
      console.log('返回数据不是有效的数组，清空历史数据');
      state2.history = []; // 如果数据不合法，清空 history
      state2.rating = 0;
    }
  } catch (error) {
    state2.error = error.response?.data?.message || '请求失败';
    console.error('请求发生错误:', error);
  }
}

</script>

<template>
  <div class="container">
    <header>
      <div class="title">
        查询选手
      </div>
      <div class="switch">
        <keep-alive>
          <button @click="handleClick">
            切换表单
            <!--(先查询handle，后查询rating)-->
          </button>
        </keep-alive>
      </div>
      <div class="query-button">
        <div>
          <!-- v-model双向绑定 -->
          <input type="text" placeholder="输入 handle" v-model="state1.handle" v-if="showHandle">
          <input type="text" placeholder="输入 handle" v-model="state2.handle" v-else>
        </div>
<!--        先调用fetchHandle的方法-->
        <div class="query" @click="fetchHandleData" v-if="showHandle">
          <input type="button" value="查询" id="query-button-small">
        </div>
<!--        后调用fetchRating的方法-->
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
        <!--
        ！！！！注意这里state.historyData.length该是个固定的常值，不该是用插值表达式引起来的变量，要不然就错了。
        所以我在下面提前处理好了state2.rating这个值。
        -->
      </div>
      <div class="answer">
        <div v-if="showHandle" class="handleResult">
          <p id="text1">handle:{{state1.handle}}</p>
          <p>rating:{{state1.rating}}</p>
          <p>rank:{{state1.rank}}</p>
        </div>
        <div v-else class="ratingResult">
          <table id="ratingHistory">
            <thead>
            <tr>
              <th>时间</th>
              <th>比赛</th>
              <th>rank</th>
              <th>变化</th>
            </tr>
            </thead>
            <tbody>
           <!-- 用v-for进行表格的处理-->
            <tr v-for="(item, index) in state2.historyData" :key="index">
              <td>{{ item.ratingUpdatedAt }}</td>
              <td>{{ item.contestName }}</td>
              <td>{{ item.rank }}</td>
              <td>{{ `${item.oldRating} -> ${item.newRating}` }}</td>
            </tr>
            </tbody>
          </table>
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
