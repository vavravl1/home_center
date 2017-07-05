import React from "react";
import Col from "react-bootstrap/lib/Col";
import ActiveSensorsSettings from "./active-sensors-settings.jsx";
import LocationSettings from "./location-settings.jsx";

class HomeCenterSettings extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
        };
    };

    render = () => {
        return <div>
            <Col xs={6} md={5}>
                <LocationSettings/>
            </Col>
            <Col xs={6} md={5}>
                <ActiveSensorsSettings/>
            </Col>
        </div>
    }
}

export default HomeCenterSettings