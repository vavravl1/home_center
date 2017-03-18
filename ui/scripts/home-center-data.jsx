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
            watering: new BcSensorState("watering")
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

        axios
            .get(wateringUrl + "?timeGranularity=" + this.state.watering.timeGranularity)
            .then(function (wateringResponse) {
                const newState = update(t.state, {
                    watering: {data: {$set: wateringResponse.data}},
                });

                t.setState(newState);
                t.tickHandler = setTimeout(t.tick.bind(t), 1000);
            }).catch(function (error) {
            console.log(error);
            t.tickHandler = setTimeout(t.tick.bind(t), 1000);
        });
    };

    render = () => {
        return <div>
            <Col xs={12} md={5}>
                <BcMeasureComponent
                    location="bridge/0"
                    phenomenon="temperature"
                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent
                    location="bridge/0"
                    phenomenon="concentration"
                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent
                    location="bridge/0"
                    phenomenon="illuminance"

                />
            </Col>
            <Col xs={12} md={5}>
                <BcMeasureComponent
                    location="bridge/0"
                    phenomenon="pressure"
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

export
default
HomeCenterData;
