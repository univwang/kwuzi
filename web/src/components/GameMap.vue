<template>
  <div ref="parent" class="gamemap">
    <canvas ref="canvas" tabindex="0"></canvas>
  </div>
</template>

<script>
import {GameMap} from "@/assets/scripts/GameMap";
import {ref, onMounted} from 'vue'
import {useStore} from "vuex";

export default {
  setup() {
    const store = useStore();
    const parent = ref(null);
    const canvas = ref(null);

    onMounted(() => {
      // Load the board image and then initialize the game
        store.commit(
            "updateGameObject",
            new GameMap(canvas.value.getContext('2d'), parent.value, store)
        );
    });

    return {
      parent,
      canvas
    }
  }
}
</script>

<style scoped>
div.gamemap {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-size: cover;

}

canvas {
  background-image: url("../assets/images/chessbackground3.jpg");
  background-size: 100% 100%;
  box-shadow: -2px -2px 2px #EFEFEF, 5px 5px 5px #B9B9B9;
  cursor: pointer;
  pointer-events: auto;

}
</style>
