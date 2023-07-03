// -- generic definitions -- //
const $ = id => document.querySelector(id);

const call = (message, persistent = false) => {
    return new Promise((resolve, reject) => window.javaCall({
        request: message,
        persistent: persistent,
        onSuccess: resolve,
        onFailure: (error_code, error_message) => reject({code: error_code, message: error_message})
    }));
};

const ifdebug = fn => {
    call('debug').then(_ => fn(), _ => { });
};

const handle = (message) => {
    ifdebug(() => console.log(message));
    switch (message) {
        case 'require-login':
            promptLogin();
            break;
        case 'cancel-incoming-call':
            cancelCallIncoming();
            break;
        default:
            break;
    }
};

addEventListener('contextmenu', event => event.preventDefault());


const loadSettings = async _ => {
    await call('audioRefreshDevices');
    let inDevices = (await call('audioListIn')).split(', ');
    let outDevices = (await call('audioListOut')).split(', ');
    let clipDevices = (await call('audioListClip')).split(', ');
    let selectedIn = parseInt(await call('audioGetIn'));
    let selectedOut = parseInt(await call('audioGetOut'));
    let selectedClip = parseInt(await call('audioGetClip'));
    let inSelect = $('#settings-select-audio-in');
    let outSelect = $('#settings-select-audio-out');
    let clipSelect = $('#settings-select-audio-clip');
    let inOptions = [];
    let outOptions = [];
    let clipOptions = [];

    let i = 0;
    inDevices.forEach(device => {
        let opt = document.createElement('option');
        opt.value = i;
        opt.innerText = device;
        opt.selected = selectedIn == i++;
        inOptions.push(opt);
    });
    i = 0;
    outDevices.forEach(device => {
        let opt = document.createElement('option');
        opt.value = i;
        opt.innerText = device;
        opt.selected = selectedOut == i++;
        outOptions.push(opt);
    });
    i = 0;
    clipDevices.forEach(device => {
        let opt = document.createElement('option');
        opt.value = i;
        opt.innerText = device;
        opt.selected = selectedClip == i++;
        clipOptions.push(opt);
    });

    $('#settings-input-display-name').value = await call('getDisplayName');
    inSelect.replaceChildren(...inOptions);
    outSelect.replaceChildren(...outOptions);
    clipSelect.replaceChildren(...clipOptions);
    $('#settings-input-audio-in-volume').value = Math.floor(await call('audioGetInVolume') / 100);
    $('#settings-input-audio-out-volume').value = Math.floor(await call('audioGetOutVolume') / 100);
    $('#settings-input-audio-clip-volume').value = Math.floor(await call('audioGetClipVolume') / 100);
    $('#settings-audio-in-volume-display').innerText = `${$('#settings-input-audio-in-volume').value}%`;
    $('#settings-audio-out-volume-display').innerText = `${$('#settings-input-audio-out-volume').value}%`;
    $('#settings-audio-clip-volume-display').innerText = `${$('#settings-input-audio-clip-volume').value}%`;
    $('#settings-modal').showModal();
};

const saveSettings = async _ => {
    call(`setDisplayName ${$('#settings-input-display-name').value}`);
    call(`audioSetIn ${$('#settings-select-audio-in').value}`);
    call(`audioSetOut ${$('#settings-select-audio-out').value}`);
    call(`audioSetClip ${$('#settings-select-audio-clip').value}`);
    call(`audioSetInVolume ${$('#settings-input-audio-in-volume').value * 100}`);
    call(`audioSetOutVolume ${$('#settings-input-audio-out-volume').value * 100}`);
    call(`audioSetClipVolume ${$('#settings-input-audio-clip-volume').value * 100}`);
};

const callIncoming = async (callerName, callerNumber) => {
    $('#call-incoming-modal-caller-name').innerText = callerName;
    $('#call-incoming-modal-caller-number').innerText = callerNumber;
    $('#call-incoming-modal').showModal();
};

const cancelCallIncoming = () => {
    $('#call-incoming-modal').close();
}

const acceptCall = async () => {
    if (await call('callInCall') === 'true') {
        $('#call-incoming-confirm-modal').showModal();
    } else await call('callAccept');
};

const confirmAcceptCall = async () => {
    await call('callAccept');
};

const declineCall = async _ => {
    await call('callDecline');
};

const promptLogin = async () => {
    $('#login-modal-username').value = await call('getUsername');
    $('#login-modal-display-name').value = await call('getDisplayName');
    $('#login-modal').showModal();
};

const login = async _ => {
    let result = await call(`setDisplayName ${$('#login-modal-display-name').value}`).then(r => { return { code: 0, message: r }}, r => r);
    if (result.code !== 0) {
        loginFailure(result);
        return;
    }
    call(`login ${$('#login-modal-username').value} ${$('#login-modal-password').value} ${$('#login-modal-persist').checked}`)
    .then(loginSuccess)
    .catch(loginFailure);
};

const loginSuccess = _ => {
    $('#login-modal-username').classList.toggle('invalid', false);
    $('#login-modal-password').classList.toggle('invalid', false);
    $('#login-modal').close();
};

const loginFailure = reason => {
    if (reason.code !== 2) {
        showError((`Internal error while logging in ${reason.message}`));
    } else {
        $('#login-modal-username').classList.toggle('invalid', !$('#login-modal-username').value);
        $('#login-modal-password').classList.toggle('invalid', true);
    }
};

const logout = async _ => {
    call('logout');
};

const callInputAppend = str => {
    $('#call-input').value += str;
};

const callInputDel = _ => {
    let value = $('#call-input').value;
    $('#call-input').value = value.substring(0, value.length - 1);
};

const startCall = _ => {
    call(`callCall ${$('#call-input').value}`);
};

const showCallOutgoing = (peerName = '', peerNumber = '') => {
    $('#call-outgoing-modal-caller-name').innerText = peerName;
    $('#call-outgoing-modal-caller-number').innerText = peerNumber;
    $('#call-outgoing-modal').showModal();
};

const setInCall = (inCall, peerName = '', peerNumber = '') => {
    if (!inCall)
        $('#call-input').value = '';
    $('#not-in-call').classList.toggle('hidden', inCall);
    $('#in-call').classList.toggle('hidden', !inCall);
    $('#call-peer-name').innerText = peerName;
    $('#call-peer-number').innerText = peerNumber;
    $('#call-outgoing-modal').close();
};

const zeroPad = (num, len) => String(num).padStart(len, '0');

const updateCallTimestamp = millis => {
    let seconds = Math.floor(millis / 1000) % 60;
    let minutes = Math.floor(millis / 1000 / 60) % 60;
    let hours = Math.floor(millis / 1000 / 60 / 60);
    $('#call-duration').innerText = `${hours}:${zeroPad(minutes, 2)}:${zeroPad(seconds, 2)}`;
};

const toggleMute = async _ => {
    await call('audioSetMute');
    $('#call-mute-btn').classList.toggle('muted', (await call('audioGetMute')) === 'true');
    $('#call-deafen-btn').classList.toggle('deafened', (await call('audioGetDeafen')) === 'true'); // update deafen button in case we un-mute (and thus un-deafen) while deafened
};

const toggleDeafen = async _ => {
    await call('audioSetDeafen');
    $('#call-mute-btn').classList.toggle('muted', (await call('audioGetMute')) === 'true'); // deafen will typically mute, too
    $('#call-deafen-btn').classList.toggle('deafened', (await call('audioGetDeafen')) === 'true');
};

const hangupCall = _ => {
    call('callHangup');
    $('#call-outgoing-modal').close();
};

const showError = reason => {
    $('#internal-error-reason').innerText = reason;
    $('#internal-error-modal').showModal();
};

// -- add listeners to dom elements -- //
document.addEventListener('DOMContentLoaded', () => {
    $('#settings-btn').addEventListener('click', loadSettings);
    $('#settings-modal-save-btn').addEventListener('click', saveSettings);
    $('#logout-btn').addEventListener('click', logout);
    $('#call-incoming-modal').addEventListener('cancel', declineCall);
    $('#call-incoming-modal-decline-btn').addEventListener('click', declineCall);
    $('#call-incoming-modal-accept-btn').addEventListener('click', acceptCall);
    $('#call-incoming-confirm-modal').addEventListener('cancel', declineCall);
    $('#call-incoming-confirm-modal-decline-btn').addEventListener('click', declineCall);
    $('#call-incoming-confirm-modal-accept-btn').addEventListener('click', confirmAcceptCall);
    $('#login-modal').addEventListener('cancel', event => event.preventDefault());
    $('#login-modal').addEventListener('keydown', event => { if (event.code === 'Enter' || event.code === 'NumpadEnter') login(); });
    $('#login-modal-login-btn').addEventListener('click', login);
    for (let i = 0; i < 10; i++)
        $(`#call-input-${i}-btn`).addEventListener('click', _ => callInputAppend(i));
    $('#call-del-btn').addEventListener('click', callInputDel);
    $('#call-call-btn').addEventListener('click', startCall);
    $('#call-hangup-btn').addEventListener('click', hangupCall);
    $('#call-mute-btn').addEventListener('click', toggleMute);
    $('#call-deafen-btn').addEventListener('click', toggleDeafen);
    $('#call-input').addEventListener('keydown', event => { if (event.code === 'Enter' || event.code === 'NumpadEnter') startCall(); });
    $('#settings-input-audio-in-volume').addEventListener('input', () => $('#settings-audio-in-volume-display').innerText = `${$('#settings-input-audio-in-volume').value}%`);
    $('#settings-input-audio-out-volume').addEventListener('input', () => $('#settings-audio-out-volume-display').innerText = `${$('#settings-input-audio-out-volume').value}%`);
    $('#settings-input-audio-clip-volume').addEventListener('input', () => $('#settings-audio-clip-volume-display').innerText = `${$('#settings-input-audio-clip-volume').value}%`);
    $('#call-outgoing-modal').addEventListener('cancel', hangupCall);
    $('#call-outgoing-modal-hangup-btn').addEventListener('click', hangupCall);
});

// -- debugging/testing -- //
ifdebug(() => {
    addEventListener('keydown', event => {
        console.log(`keydown: ${event.code}`);
        if (event.code === 'F5') location.reload();
    });
});
