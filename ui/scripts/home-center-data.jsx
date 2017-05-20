import React from "react";
import WateringComponent from "./watering-component.jsx";
import Sensor from "./sensor/sensor.jsx";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import update from "react-addons-update";

class HomeCenterData extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensors: [],
            bigBcSensor: null,
        }
    };

    componentDidMount = () => {
        let bcSensorReading = document.getElementById('bcSensorReading').value;
        let t = this;
        axios
            .get(bcSensorReading)
            .then(function (bcSensors) {
                const newState = update(t.state, {
                    bcSensors: {$set: bcSensors.data},
                });
                t.setState(newState);
            })
    };

    makeBcSensorBig = (sensor) => {
        const newState = update(this.state, {
            bigBcSensor: {$set: sensor},
        });
        this.setState(newState);
    };

    makeBcSensorSmall = () => {
        const newState = update(this.state, {
            bigBcSensor: {$set: null}
        });
        this.setState(newState);
    };

    render = () => {
        if (this.state.bigBcSensor !== null) {
            return <Col xs={10} md={10}>
                <Sensor
                    sensor={this.state.bigBcSensor}
                    makeSmallCallback={this.makeBcSensorSmall}
                />
            </Col>
        } else {
            let bcSensorsComponents = this.state.bcSensors.map(oneSensor =>
                <Col xs={12} md={5} key={oneSensor.location.address+ "-" + oneSensor.name}>
                    <Sensor
                        sensor={oneSensor}
                        measuredPhenomenon={oneSensor.measuredPhenomenon}
                        makeBigCallback={this.makeBcSensorBig}
                    />
                </Col>
            );
            return <div>
                {bcSensorsComponents}
                <Col xs={12} md={5}>
                    <WateringComponent />
                </Col>
            </div>
        }
    }
}

export default HomeCenterData;
