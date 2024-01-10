import {AcGameObject} from "./AcGameObject";

export class Player extends AcGameObject {
    constructor(info, gamemap) {
        super();

        this.id = info.id;
        this.color = info.color;
        this.gamemap = gamemap;
        this.ctx = this.gamemap.ctx;

        this.L = 15
        this.direction = -1;  // -1表示没有指令，一个数代表棋子的位置
        this.status = "idle";  // idle表示静止，move表示正在移动，die表示死亡
        this.step = 0;  // 表示回合数
        this.chesses = []
        this.time = 60;
    }

    start() {

    }

    set_direction(d) {
        this.direction = d;
    }

    set_time(t) {
        this.time = t;
    }

    next_step() {  // 下一步
        if (this.direction === -1) return
        const d = this.direction
        this.direction = -1
        const x = Math.floor(d / this.L)
        const y = d % this.L;
        if(this.color === "black") {
            this.gamemap.g[x][y] = 1;
        } else {
            this.gamemap.g[x][y] = 2;
        }
        this.chesses.push({row: x, col: y, color: this.color})
        this.status = "move";
        this.step++;
    }

    update_move() {
        this.status = "idle"
    }


    update() {  // 每一帧执行一次
        if (this.status === 'move') {
            this.update_move();
        }

        this.render();
    }

    render() {

    }
}
