import * as L from 'leaflet';

const sensorIconGreen = new L.Icon({
    iconUrl: require('../images/sensor_green.png').default,
    iconRetinaUrl: require('../images/sensor_green.png').default,
    iconAnchor: null,
    popupAnchor: [0, -6],
    shadowUrl: null,
    shadowSize: null,
    shadowAnchor: null,
    iconSize: new L.Point(20, 20),
});

const sensorIconRed = new L.Icon({
    iconUrl: require('../images/sensor_red.png').default,
    iconRetinaUrl: require('../images/sensor_red.png').default,
    iconAnchor: null,
    popupAnchor: [0, -6],
    shadowUrl: null,
    shadowSize: null,
    shadowAnchor: null,
    iconSize: new L.Point(20, 20),
});

export { sensorIconGreen };
export { sensorIconRed };