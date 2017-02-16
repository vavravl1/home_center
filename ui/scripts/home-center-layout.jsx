import React from "react";
import WateringComponent from "./watering-component.jsx";
import BcMeasureComponent from "./bc-measure-component.jsx";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import update from "react-addons-update";
import BcSensorState from "./bc-sensor-state.js";

class HomeCenterLayout extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            watering:new BcSensorState("watering"),
            temperature:new BcSensorState("temperature"),
            carbonDioxide:new BcSensorState("carbonDioxide"),
            illuminance:new BcSensorState("illuminance"),
            relativeHumidity:new BcSensorState("relativeHumidity"),
        }
    };

    componentDidMount = () => {
        this.tick();
    };

    changeTimeGranularity(sensorName, value) {
        this.setState(update(this.state, {[sensorName]:{timeGranularity: {$set: value}}}));
    };

    tick = () => {
        let t = this;
        let wateringUrl = document.getElementById('wateringBackendUrl').value;
        let bcSensorReading = document.getElementById('bcSensorReading').value;

        axios.all([
            axios.get(wateringUrl + "?timeGranularity=" + this.state.watering.timeGranularity),
            axios.get(bcSensorReading + "temperature?timeGranularity=" + this.state.temperature.timeGranularity),
            axios.get(bcSensorReading + "concentration?timeGranularity=" + this.state.carbonDioxide.timeGranularity),
            axios.get(bcSensorReading + "illuminance?timeGranularity=" + this.state.illuminance.timeGranularity),
            axios.get(bcSensorReading + "relative-humidity?timeGranularity=" + this.state.relativeHumidity.timeGranularity)
        ]).then(axios.spread(function (wateringResponse, temperatureResponse, carbonDioxide, illuminance, relativeHumidity) {
            setTimeout(t.tick, 1000);
            const newState = update(t.state, {
                watering: {data: {$set: wateringResponse.data}},
                temperature: {data:{$set: temperatureResponse.data}},
                carbonDioxide: {data:{$set: carbonDioxide.data}},
                illuminance: {data:{$set: illuminance.data}},
                relativeHumidity: {data:{$set: relativeHumidity.data}}
            });
            t.setState(newState);
        }));
    };

    render = () => {
        return <div>
            <Col xs={6} md={5}>
                <BcMeasureComponent measure={this.state.temperature.data}
                                    sensorLocation="upstairs corridor"
                                    sensorName="temperature"
                                    activeTimeGranularity={this.state.temperature.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "temperature")}
                />
            </Col>
            <Col xs={6} md={5}>
                <BcMeasureComponent measure={this.state.carbonDioxide.data}
                                    sensorLocation="upstairs corridor"
                                    sensorName="carbon dioxide"
                                    activeTimeGranularity={this.state.carbonDioxide.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "carbonDioxide")}/>
            </Col>
            <Col xs={6} md={5}>
                <BcMeasureComponent measure={this.state.illuminance.data}
                                    sensorLocation="upstairs corridor"
                                    sensorName="illuminance"
                                    activeTimeGranularity={this.state.illuminance.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "illuminance")}
                />
            </Col>
            <Col xs={6} md={5}>
                <BcMeasureComponent measure={this.state.relativeHumidity.data}
                                    sensorLocation="upstairs corridor"
                                    sensorName="relative humidity"
                                    activeTimeGranularity={this.state.relativeHumidity.timeGranularity}
                                    timeGranularityCallback={this.changeTimeGranularity.bind(this, "relativeHumidity")}
                />
            </Col>
            <Col xs={6} md={5}>
                <WateringComponent watering={this.state.watering.data}
                                   activeTimeGranularity={this.state.watering.timeGranularity}
                                   timeGranularityCallback={this.changeTimeGranularity.bind(this, "watering")}
                />
            </Col>
        </div>
    }
}

export default HomeCenterLayout;
