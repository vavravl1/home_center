import React from "react";
import WateringComponent from "./watering-component.jsx";
import BcSensor from "./bc-sensor.jsx";
import Col from "react-bootstrap/lib/Col";
import axios from "axios";
import update from "react-addons-update";

class HomeCenterData extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            bcSensors: []
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
                console.log("Setting state to " + JSON.stringify(newState));
            })
    };

    render = () => {
        let bcSensorsComponents = this.state.bcSensors.map(oneSensor =>
            <Col xs={12} md={5} key={oneSensor.location + '/' + oneSensor.phenomenon}>
                <BcSensor
                    location={oneSensor.location}
                    phenomenon={oneSensor.phenomenon}
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

export default HomeCenterData;
