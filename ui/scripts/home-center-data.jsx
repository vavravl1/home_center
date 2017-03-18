import React from "react";
import WateringComponent from "./watering-component.jsx";
import BcSensor from "./bc-sensor.jsx";
import Col from "react-bootstrap/lib/Col";

class HomeCenterData extends React.Component {
    render = () => {
        return <div>
            <Col xs={12} md={5}>
                <BcSensor
                    location="bridge/0"
                    phenomenon="temperature"
                />
            </Col>
            <Col xs={12} md={5}>
                <BcSensor
                    location="bridge/0"
                    phenomenon="concentration"
                />
            </Col>
            <Col xs={12} md={5}>
                <BcSensor
                    location="bridge/0"
                    phenomenon="illuminance"

                />
            </Col>
            <Col xs={12} md={5}>
                <BcSensor
                    location="bridge/0"
                    phenomenon="pressure"
                />
            </Col>
            <Col xs={12} md={5}>
                <WateringComponent />
            </Col>
        </div>
    }
}

export default HomeCenterData;
