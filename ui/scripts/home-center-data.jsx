import React from "react";
import WateringComponent from "./watering-component.jsx";
import BcMeasureComponent from "./bc-measure-component.jsx";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import update from "react-addons-update";
import BcSensorState from "./bc-sensor-state.js";

class HomeCenterData extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            watering: new BcSensorState("watering"),
            temperature: new BcSensorState("temperature"),
            carbonDioxide: new BcSensorState("carbonDioxide"),
            illuminance: new BcSensorState("illuminance"),
            relativeHumidity: new BcSensorState("relativeHumidity"),
            pressure: new BcSensorState("pressure")
        };
    };

    componentDidMount = () => {
        this.tick();
    };

    componentWillUnmount = () => {
        if (this.tickHandler !== null) {
            clearTimeout(this.tickHandler);
        }
    };

    changeTimeGranularity(sensorName, value) {
        this.setState(update(this.state, {[sensorName]: {timeGranularity: {$set: value}}}));
    };

    tick = () => {
        let t = this;
        let wateringUrl = document.getElementById('wateringBackendUrl').value;
        let bcSensorReading = document.getElementById('bcSensorReading').value;

        axios.all([
            axios.get(wateringUrl + "?timeGranularity=" + this.state.watering.timeGranularity),
            axios.get(bcSensorReading + "bridge/0/temperature?timeGranularity=" + this.state.temperature.timeGranularity),
            axios.get(bcSensorReading + "bridge/0/concentration?timeGranularity=" + this.state.carbonDioxide.timeGranularity),
            axios.get(bcSensorReading + "bridge/0/illuminance?timeGranularity=" + this.state.illuminance.timeGranularity),
            axios.get(bcSensorReading + "bridge/0/relative-humidity?timeGranularity=" + this.state.relativeHumidity.timeGranularity),
            axios.get(bcSensorReading + "bridge/0/pressure?timeGranularity=" + this.state.pressure.timeGranularity)
        ]).then(axios.spread(function (wateringResponse, temperatureResponse, carbonDioxide, illuminance, relativeHumidity, pressure) {
            const newState = update(t.state, {
                watering: {data: {$set: wateringResponse.data}},
                temperature: {data: {$set: temperatureResponse.data}},
                carbonDioxide: {data: {$set: carbonDioxide.data}},
                illuminance: {data: {$set: illuminance.data}},
                relativeHumidity: {data: {$set: relativeHumidity.data}},
                pressure: {data: {$set: pressure.data}},
            });

            t.setState(newState);
            t.tickHandler = setTimeout(t.tick.bind(t), 1000);
        })).catch(function (error) {
            console.log(error);
            t.tickHandler = setTimeout(t.tick.bind(t), 1000);
        });
    };

    render = () => {
        return <div>
            <Col xs={12} md={5}>
                <BcMeasureComponent measure={this.state.temperature.data}
                                    sensorName="temperature"
                                    activeTimeGranularity={this.state.temperature.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "temperature")}
                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent measure={this.state.carbonDioxide.data}
                                    sensorName="carbon dioxide"
                                    activeTimeGranularity={this.state.carbonDioxide.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "carbonDioxide")}/>
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent measure={this.state.illuminance.data}
                                    sensorName="illuminance"
                                    activeTimeGranularity={this.state.illuminance.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "illuminance")}
                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent measure={this.state.relativeHumidity.data}
                                    sensorName="relative humidity"
                                    activeTimeGranularity={this.state.relativeHumidity.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "relativeHumidity")}
                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent measure={this.state.pressure.data}
                                   sensorName="pressure"
                                   activeTimeGranularity={this.state.pressure.timeGranularity}
                                   timeGranularityCallback={this.changeTimeGranularity.bind(this, "pressure")}
                />
            </Col>
            <Col xs={12} md={5}>
                <WateringComponent watering={this.state.watering.data}
                                   activeTimeGranularity={this.state.watering.timeGranularity}
                                   timeGranularityCallback={this.changeTimeGranularity.bind(this, "watering")}
                />
            </Col>
        </div>
    }
}

export default HomeCenterData;
