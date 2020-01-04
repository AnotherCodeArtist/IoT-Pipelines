import * as L from 'leaflet';

const bounds = [[54.559322, -5.767822], [56.1210604, -3.021240]];

const rectangle_green = new L.rectangle(bounds, {color: "#ff7800", weight: 1})
const rectangle_red = new L.rectangle(bounds, {color: "#ff7800", weight: 1})

export { rectangle_green };
export { rectangle_red };