export default {
    state: {
        is_record: false,
        a_steps: "",
        b_steps: "",
        a_player: "",
        b_player: "",
        record_loser: "",
        record_over: false,
    },
    getters: {},
    mutations: {
        updateIsRecord(state, is_record) {
            state.is_record = is_record;
        },
        updateSteps(state, data) {
            state.a_steps = data.a_steps;
            state.b_steps = data.b_steps;
        },
        updateRecordLoser(state, loser) {
            state.record_loser = loser;
        },
        updateRecordPlayer(state, data) {
            state.a_player = data.a_player;
            state.b_player = data.b_player;
        }

    },
    actions: {},
    modules: {}
}