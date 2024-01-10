<template>
  <!--    <PlayGround />-->
  <div class="game-container">
    <!-- 游戏区域 -->
    <div class="game-area">
      <PlayGround/>
    </div>

    <!-- 信息展示区域 -->
    <div class="info-area">
      <RecordRightBoard/>
    </div>
  </div>
</template>

<script>
import PlayGround from '../../components/PlayGround.vue';
import RecordRightBoard from "@/components/RecordRightBoard.vue";

export default {
  components: {
    RecordRightBoard,
    PlayGround,
  },
  setup() {

  }
}
</script>

<style scoped>

.game-container {
  display: flex;
  height: 100%; /* 根据需要调整 */
}

.game-area {
  flex: 1; /* 游戏区域占据更多空间 */
  /* 根据需要添加其他样式，如padding、margin等 */
}

.info-area {
  flex: 1; /* 信息展示区域占据剩余空间 */
  /* 根据需要添加其他样式，如padding、margin、background等 */
}
</style>