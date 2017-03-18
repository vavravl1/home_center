import React, {Component, PropTypes} from "react";
import axios from "axios";
import update from "react-addons-update";
import BcSensorView from "./bc-sensor-view.jsx";

class BcSensor extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            timeGranularity: "ByHour",
            data: []
        }
    };

    componentDidMount = () => {
        this.tick();
    };

    componentWillUnmount = () => {
        if (this.tickHandler !== null) {
            clearTimeout(this.tickHandler);
        }
    };

    tick = () => {
        let t = this;
        let bcSensorReading = document.getElementById('bcSensorReading').value;
        axios
            .get(
                bcSensorReading + this.props.location + "/" +
                this.props.phenomenon + "?timeGranularity=" + this.state.timeGranularity
            )
            .then(function(measurement) {
                const newState = update(t.state, {
                    data: {$set: measurement.data},
                });
                t.setState(newState);
                t.tickHandler = setTimeout(t.tick.bind(t), 1000);
            })
            .catch(function (error) {
                console.log(error);
                t.tickHandler = setTimeout(t.tick.bind(t), 1000);
            })
    };

    timeGranularityChangedCallback = (value) => {
        const newState = update(this.state, {
            timeGranularity: {$set: value},
        });
        this.setState(newState);
    };

    render = () => {
        return <BcSensorView
            location={this.props.location}
            data={this.state.data}
            timeGranularity = {this.state.timeGranularity}
            timeGranularityChangedCallback = {this.timeGranularityChangedCallback}
        />
    };
}

BcSensor.PropTypes = {
    phenomenon: PropTypes.string.isRequired,
    location: PropTypes.string.isRequired,
};


export default BcSensor;