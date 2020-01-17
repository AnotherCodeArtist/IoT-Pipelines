import 'leaflet/dist/leaflet.css'
import './css/map.css'
import 'leaflet-defaulticon-compatibility/dist/leaflet-defaulticon-compatibility.webpack.css';

import {Map, TileLayer, Marker, Popup, Rectangle} from 'react-leaflet'

import 'leaflet-defaulticon-compatibility';
import {sensorIconGreen, sensorIconRed} from './icons/sensor_icon';
//import { rectangle_green } from './rectangle';

import React, {Component} from 'react';
import {PanelProps, PanelPlugin} from "@grafana/ui";


type State = {
    lat: number,
    lng: number,
    zoom: number,
}


export class MyPanel extends Component<PanelProps, State> {

    state = {
        lat: 47.063,
        lng: 15.44,
        zoom: 12,
    }

    // Change these coordinates to your custom starting point!

    startcoords = {
        lat: 47.063,
        lng: 15.44,
    };


    render() {
        const position = [this.state.lat, this.state.lng]

        // Calculate decimal degrees from given sensor-distance
        const sensor_distance = 100; // Distance in meters // Change this value to your desired distance between sensors on the grid!
        const radius_earth_mean = 6371000; //Average radius of the earth
        const radius_earth_of_alpha = radius_earth_mean * Math.sin((this.startcoords.lat) * Math.PI / 180);
        const lat_distance = sensor_distance * (180 / (radius_earth_mean * Math.sin((this.startcoords.lat) * Math.PI / 180) * Math.PI));
        const lng_distance = sensor_distance * (180 / (radius_earth_of_alpha * Math.PI)) / Math.cos((this.startcoords.lat) * Math.PI / 180); //implemented ratio difference
        //Calculate End

        // GET JSON LISTS

        //id array
        const column_id_field = this.props.data.series[0].fields.filter(function (s) {
            return s.name == "id";
        });
        const column_id_values = column_id_field[0].values.toArray();
        const column_id_values_set = Array.from(new Set(column_id_values));
        //console.log(column_id_values);

        //avgPM2 array
        const column_pm_field = this.props.data.series[0].fields.filter(function (s) {
            return s.name == "avgPM2";
        });
        const column_pm_values = column_pm_field[0].values.toArray();
        //console.log(column_pm_values);

        //lat array
        const column_lat_field = this.props.data.series[0].fields.filter(function (s) {
            return s.name == "lat";
        });
        const column_lat_values = column_lat_field[0].values.toArray();
        const column_lat_values_converted = column_lat_values.map(x => this.startcoords.lat + x * lat_distance);
        //console.log(column_lat_values);

        //long array
        const column_long_field = this.props.data.series[0].fields.filter(function (s) {
            return s.name == "long";
        });
        const column_long_values = column_long_field[0].values.toArray();
        const column_long_values_converted = column_long_values.map(y => this.startcoords.lng + y * lng_distance);
        //console.log(column_long_values);

        //area array
        const column_area_field = this.props.data.series[1].fields.filter(function (s) {
            return s.name == "area";
        });
        const column_area_values = column_area_field[0].values.toArray();

        // tooHigh array
        const column_tooHigh_field = this.props.data.series[1].fields.filter(function (s) {
            return s.name == "tooHigh";
        });
        const column_tooHigh_values = column_tooHigh_field[0].values.toArray();
        console.log(column_tooHigh_values);

        // GET JSON LISTS END


        // rectangle calculations
        const number_of_areas = 4;
        const areas_per_side = Math.sqrt(number_of_areas);
        const dual_usages = areas_per_side - 1;
        const sensors_per_side = Math.sqrt(column_id_values_set.length);
        const sensors_per_area_side = (sensors_per_side + dual_usages) / areas_per_side;

        const column_area_values_converted = []; // Object-list of given simple-coordinates from area measurement

        for (let x = 0; x < column_area_values.length; x++) {
            const start_lat = this.startcoords.lat + ((sensors_per_area_side - 1) * lat_distance * parseInt(column_area_values[x][1]));
            const start_lng = this.startcoords.lng + ((sensors_per_area_side - 1) * lng_distance * parseInt(column_area_values[x][3]));
            const end_lat = start_lat + (sensors_per_area_side - 1) * lat_distance;
            const end_lng = start_lng + (sensors_per_area_side - 1) * lng_distance;
            column_area_values_converted.push({start_lat, start_lng, end_lat, end_lng})
        }

        console.log(column_area_values_converted);

        // rectangle calculations end


        // object-array for sensors
        const completeArray = [];

        for (let x = 0; x < column_pm_values.length; x++) {
            const lat = column_lat_values_converted[x];
            const lng = column_long_values_converted[x];
            completeArray.push({lat, lng})
        }

        //object-array for sensors END

        function toggleMarker(i) {
            if (i < 15) {
                return sensorIconGreen;
            } else {
                return sensorIconRed;
            }
        }

        function toggleArea(a) {
            if (a < 2) {
                return {
                    color: "green",
                    weight: 0.2,
                };
            } else {
                return {
                    color: "red",
                    weight: 0.2,
                };
            }
        }


        return (

            <Map center={position} zoom={this.state.zoom}>
                <TileLayer
                    attribution='&amp;copy <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {column_area_values_converted.map((s, idx) => (
                    <Rectangle bounds={[[s.start_lat, s.start_lng], [s.end_lat, s.end_lng]]} {...toggleArea(column_tooHigh_values[idx])}>
                        <Popup>
                            In diesem Bereich melden {column_tooHigh_values[idx]} Sensoren einen zu hohen Wert!
                        </Popup>
                    </Rectangle>))}


                {completeArray.map((s, idx) => (
                    <Marker position={[s.lat, s.lng]} icon={toggleMarker(column_pm_values[idx])}>
                        <Popup>
                            <b>SensorID:</b> {column_id_values[idx]}<br/>
                            <b>avgPM2:</b> {column_pm_values[idx]}<br/>
                        </Popup>
                    </Marker>)
                )}
                <Marker position={[this.state.lat - 1 / 300, this.state.lng - 1 / 300]}>
                    <Popup>
                        {JSON.stringify(this.props.data)}<br/>
                    </Popup>
                </Marker>


            </Map>)
    }
}

// export has to be "plugin" or "PanelCtrl" !
export const plugin = new PanelPlugin(MyPanel);
