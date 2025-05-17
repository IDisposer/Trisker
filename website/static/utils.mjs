export function copy(object) {
    return JSON.parse(JSON.stringify(object));
}

export function sleep(millis) {
    return new Promise((resolve) => {
        setTimeout(() => resolve(), millis);
    });
}
