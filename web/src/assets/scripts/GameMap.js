import {AcGameObject} from "./AcGameObject";
import {Player} from "@/assets/scripts/Player";

export class GameMap extends AcGameObject {
    constructor(ctx, parent, store) {
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.store = store;
        this.L = 15;

        this.rows = 15;
        this.cols = 15;
        this.hrow = 7;
        this.hcol = 7;
        this.snakes = [
            new Player({id: 0, color: "black"}, this),
            new Player({id: 1, color: "white"}, this),
        ];
        this.g = []
        for (let i = 0; i < this.rows; i++) {
            this.g.push(new Array(this.rows).fill(0));
        }
    }


    add_listening_events() {
        if (this.store.state.record.is_record) {
            let k1 = 0;
            let k2 = 0;
            const a_steps = this.store.state.record.a_steps;
            const b_steps = this.store.state.record.b_steps;
            const a_stepsArray = a_steps.split('-');
            const b_stepsArray = b_steps.split('-');
            const a_stepsArrayNumeric = a_stepsArray.map(step => parseInt(step));
            const b_stepsArrayNumeric = b_stepsArray.map(step => parseInt(step));
            const loser = this.store.state.record.record_loser;
            const [snake0, snake1] = this.snakes;
            const interval_id = setInterval(() => {
                if (k1 >= a_stepsArrayNumeric.length && k2 >= b_stepsArrayNumeric.length || a_steps === "" || b_steps === "") {
                    if (loser === "all" || loser === "A") {
                        snake0.status = "die"
                    }
                    if (loser === "all" || loser === "B") {
                        snake1.status = "die"
                    }
                    this.store.state.record.record_over = true;
                    clearInterval(interval_id);
                } else {
                    if (k1 < a_stepsArrayNumeric.length) {
                        snake0.set_direction(parseInt(a_stepsArrayNumeric[k1]));
                        snake0.next_step();
                        k1++;
                    }
                    if (k2 < b_stepsArrayNumeric.length) {
                        snake1.set_direction(parseInt(b_stepsArrayNumeric[k2]));
                        snake1.next_step();
                        k2++;
                    }
                }
            }, 300)

        } else {
            this.ctx.canvas.focus();
            // 监听鼠标移动事件
            this.ctx.canvas.addEventListener("mousemove", e => {
                // 计算鼠标位置对应的坐标格
                this.calculateCell(e);
            });

            // 监听鼠标点击事件
            this.ctx.canvas.addEventListener("click", e => {
                // 计算鼠标点击对应的坐标格
                this.calculateCell(e, true); // true 表示是点击事件
            });
        }
    }


    calculateCell(e, isClick = false) {
        const rect = this.ctx.canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        // 考虑到棋盘的padding
        const padding = this.L / 2;

        // 计算鼠标当前位置对应的坐标格
        const col = Math.round((x - padding) / this.L);
        const row = Math.round((y - padding) / this.L);

        // 确保坐标在棋盘格子范围内
        if (row >= 0 && row < this.rows && col >= 0 && col < this.cols) {
            if (isClick) {
                // 如果是点击事件，处理点击逻辑
                // const newChess = new Chess(row, col, "white"); // currentColor 根据当前玩家变化
                // this.chesses.push(newChess);
                this.store.state.pk.socket.send(JSON.stringify({
                    event: "move",
                    direction: row * this.cols + col,
                }));
            } else {
                // 如果是鼠标移动事件，处理鼠标移动逻辑
                // 例如：高亮显示鼠标下的格子
                this.hrow = row;
                this.hcol = col;
            }
        }
    }

    highlightCell(row, col) {
        // 首先清除之前的高亮（如果有的话）
        // 计算交叉点的中心坐标
        const centerX = col * this.L + (this.L / 2);
        const centerY = row * this.L + (this.L / 2);

        // 绘制红色圆圈进行高亮
        this.ctx.beginPath();
        this.ctx.arc(centerX, centerY, this.L / 2 * 0.8, 0, 2 * Math.PI);
        this.ctx.strokeStyle = "red";
        this.ctx.lineWidth = 2; // 加粗线条，可以根据
        this.ctx.stroke();
    }

    start() {
        this.create_board();
        this.add_listening_events();
    }

    update_size() {
        this.ctx.canvas.focus();//自动聚焦
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
        this.create_board();
    }

    check_ready() {  // 判断两条蛇是否都准备好下一回合了
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
        }
        for (const snake of this.snakes) {
            if (snake.direction !== -1) return true;
        }
        return false;
    }

    next_step() {
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    update() {
        this.update_size();
        if (this.check_ready()) {
            this.next_step();
        }
        this.render();
    }

    create_board() {
        // 设置棋盘网格颜色
        this.ctx.strokeStyle = "#000000";
        this.ctx.lineWidth = 1;

        // 计算棋盘网格的起始和结束坐标，确保四周都有空隙
        const padding = this.L / 2;
        const boardSize = this.L * (this.rows - 1);

        // 绘制棋盘网格
        for (let i = 0; i < this.rows; i++) {
            this.ctx.beginPath();
            this.ctx.moveTo(padding, i * this.L + padding);
            this.ctx.lineTo(boardSize + padding, i * this.L + padding);
            this.ctx.stroke();
        }
        for (let i = 0; i < this.cols; i++) {
            this.ctx.beginPath();
            this.ctx.moveTo(i * this.L + padding, padding);
            this.ctx.lineTo(i * this.L + padding, boardSize + padding);
            this.ctx.stroke();
        }

        // 绘制特殊点，考虑到棋盘边缘的空隙
        const points = [[3, 3], [7, 7], [11, 11], [3, 11], [11, 3]];

        this.ctx.fillStyle = "#000000";
        for (let point of points) {
            this.ctx.beginPath();
            // 加上padding以考虑边缘的空隙
            this.ctx.arc(point[0] * this.L + padding, point[1] * this.L + padding, 3, 0, Math.PI * 2, true);
            this.ctx.fill();
        }
    }


    drawChess(chess, step = 0) {
        const centerX = chess.col * this.L + (this.L / 2);
        const centerY = chess.row * this.L + (this.L / 2);
        const radius = this.L / 2 * 0.8; // 棋子大小为格子宽度的 80%

        // 添加棋子的阴影
        this.ctx.shadowOffsetX = 2;
        this.ctx.shadowOffsetY = 2;
        this.ctx.shadowBlur = 3;
        this.ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';

        // 创建径向渐变来模拟光泽
        const gradient = this.ctx.createRadialGradient(
            centerX - radius / 3,
            centerY - radius / 3,
            radius / 3,
            centerX,
            centerY,
            radius
        );

        if (chess.color === 'black') {
            // 为黑棋设置渐变颜色，使用较深的灰色减弱光泽效果
            gradient.addColorStop(0, '#555');  // 较深的灰色作为光泽
            gradient.addColorStop(1, '#000');
        } else {
            // 为白棋设置渐变颜色
            gradient.addColorStop(0, '#fff');
            gradient.addColorStop(1, '#ccc');
        }

        // 绘制棋子
        this.ctx.beginPath();
        this.ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
        this.ctx.fillStyle = gradient;
        this.ctx.fill();
        // 清除之前设置的阴影，以免影响后续绘图
        this.ctx.shadowOffsetX = 0;
        this.ctx.shadowOffsetY = 0;
        this.ctx.shadowBlur = 0;


        if (this.store.state.record.is_record) {
            // 设置文本颜色
            this.ctx.fillStyle = chess.color === 'black' ? '#fff' : '#000';

            // 设置文本的字体和大小
            this.ctx.font = '24px Arial'; // 修改字体大小为 24px，可以根据需要调整大小

            // 计算文本的宽度和高度
            const textWidth = this.ctx.measureText(step.toString()).width;
            const textHeight = 24; // 根据字体大小调整高度，确保在棋子中央

            // 计算文本的绘制位置，使其位于棋子中央
            const textX = centerX - textWidth / 2;
            const textY = centerY + textHeight / 4; // 调整 Y 坐标以垂直居中

            // 在棋子中央绘制 step 数字
            this.ctx.fillText(step.toString(), textX, textY);
        }
    }

    drawChesses() {
        for (const snake of this.snakes) {
            for (let i = 0; i < snake.chesses.length; i++) {
                const chess = snake.chesses[i];
                this.drawChess(chess, i + 1);
            }

            // 特别标记每个玩家的最后一步棋子
            if (snake.chesses.length > 0 && !this.store.state.record.is_record) {
                const lastChess = snake.chesses[snake.chesses.length - 1];
                this.highlightLastMove(lastChess);
            }
        }


    }

    drawfive() {
        // 遍历棋盘上的每个格子
        for (let row = 0; row < this.rows; row++) {
            for (let col = 0; col < this.cols; col++) {
                // 检查每个方向（水平、垂直、两个对角线）
                const directions = [
                    { dr: 0, dc: 1 }, // 水平方向
                    { dr: 1, dc: 0 }, // 垂直方向
                    { dr: 1, dc: 1 }, // 右下对角线
                    { dr: 1, dc: -1 } // 左下对角线
                ];

                for (let direction of directions) {
                    let count = 1;
                    let r = row, c = col;
                    const color = this.g[row][col]; // 当前棋子的颜色

                    if (color === 0) continue; // 空格，跳过

                    // 沿着一个方向检查是否有五个连续的同色棋子
                    // eslint-disable-next-line no-constant-condition
                    while (1 === 1) {
                        r += direction.dr;
                        c += direction.dc;

                        if (r < 0 || r >= this.rows || c < 0 || c >= this.cols || this.g[r][c] !== color) {
                            break;
                        }
                        count++;
                    }

                    if (count === 5) {
                        // 找到五子连珠，画线
                        this.drawLine(row, col, r - direction.dr, c - direction.dc, color);
                        return; // 找到一个五子连珠就可以结束方法
                    }
                }
            }
        }
    }

    drawLine(startRow, startCol, endRow, endCol, color) {
        const padding = this.L / 2;
        const startX = startCol * this.L + padding;
        const startY = startRow * this.L + padding;
        const endX = endCol * this.L + padding;
        const endY = endRow * this.L + padding;

        // 创建径向渐变
        const gradient = this.ctx.createLinearGradient(startX, startY, endX, endY);
        gradient.addColorStop(0, 'rgba(255, 215, 0, 1)');  // 渐变起始颜色（金色）
        gradient.addColorStop(1, color === 'black' ? 'rgba(0, 0, 0, 1)' : 'rgba(255, 255, 255, 1)'); // 渐变结束颜色（黑色或白色）

        // 添加线条阴影效果
        this.ctx.shadowOffsetX = 3;
        this.ctx.shadowOffsetY = 3;
        this.ctx.shadowBlur = 5;
        this.ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';

        this.ctx.beginPath();
        this.ctx.moveTo(startX, startY);
        this.ctx.lineTo(endX, endY);
        this.ctx.strokeStyle = gradient;
        this.ctx.lineWidth = 5; // 增加线条宽度
        this.ctx.stroke();

        // 清除阴影，以免影响后续绘图
        this.ctx.shadowOffsetX = 0;
        this.ctx.shadowOffsetY = 0;
        this.ctx.shadowBlur = 0;
    }

    highlightLastMove(chess) {
        const centerX = chess.col * this.L + (this.L / 2);
        const centerY = chess.row * this.L + (this.L / 2);
        const radius = this.L / 2 * 0.2; // 中心点的大小为棋子半径的 20%

        // 开始绘制
        this.ctx.beginPath();
        this.ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);

        // 中心点的颜色与棋子颜色相反
        this.ctx.fillStyle = chess.color === 'black' ? '#fff' : '#000';
        this.ctx.fill();
    }
    render() {
        // 清除画布
        // this.ctx.clearRect(0, 0, this.ctx.canvas.width, this.ctx.canvas.height);
        this.highlightCell(this.hrow, this.hcol);
        this.drawChesses();
        this.drawfive();
    }
}