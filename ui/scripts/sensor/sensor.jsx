import React from "react";
import axios from "axios";
import update from "react-addons-update";
import SensorView from "./sensor-view.jsx";
import SensorBarView from "./sensor-bar-view.jsx";
import PropTypes from "prop-types";


class Sensor extends React.Component {

    constructor(props) {
        super(props);
        const cancelToken = axios.CancelToken;
        this.state = {
            timeGranularity: this.props.sensor.areAllMeasuredPhenomenonsSingleValue ? "ByDay" : "ByHour",
            measuredPhenomenons: [],
            tickHandler: null,
            source: cancelToken.source(),
        }
    };

    componentDidMount = () => {
        this.tick.call(this);
    };

    componentWillUnmount = () => {
        if (this.state.tickHandler != null) {
            clearTimeout(this.state.tickHandler);
        }
        this.state.source.cancel("Component is not rendered anymore");
    };

    tick = () => {
        let t = this;
        let sensorReading = document.getElementById('bcSensorReading').value;
        axios
            .get(
                sensorReading + this.props.sensor.location.address + "/" +
                this.props.sensor.name + "?timeGranularity=" + this.state.timeGranularity +
                ((!!this.props.makeSmallCallback) ? "&big=true" : "&big=false"), {
                    cancelToken: this.state.source.token
                }
            )
            .then(function (_measuredPhenomenons) {
                const tickHandler = setTimeout(t.tick.bind(t), 1000);
                const newState = update(t.state, {
                    measuredPhenomenons: {
                        $set: _measuredPhenomenons.data
                    },
                    tickHandler: {$set:  tickHandler}
                });
                t.setState(newState);
            })
            .catch(function (error) {
                console.log(error);
            })
    };

    timeGranularityChangedCallback = (value) => {
        const newState = update(this.state, {
            timeGranularity: {$set: value},
        });
        this.setState(newState);
    };

    render = () => {
        const allDataAreSingleValue = this.state.measuredPhenomenons
            .every(phenomenon => phenomenon.aggregationStrategy === 'singleValue');
        if(allDataAreSingleValue) {
            return <SensorBarView
                sensor={this.props.sensor}
                measuredPhenomenons={this.state.measuredPhenomenons}
                timeGranularity={this.state.timeGranularity}
                timeGranularityChangedCallback={this.timeGranularityChangedCallback}
                makeBigCallback={this.props.makeBigCallback}
                makeSmallCallback={this.props.makeSmallCallback}
            />
        } else {
            return <SensorView
                sensor={this.props.sensor}
                measuredPhenomenons={this.state.measuredPhenomenons}
                timeGranularity={this.state.timeGranularity}
                timeGranularityChangedCallback={this.timeGranularityChangedCallback}
                makeBigCallback={this.props.makeBigCallback}
                makeSmallCallback={this.props.makeSmallCallback}
            />
        }
    };
}

Sensor.propTypes = {
    sensor: PropTypes.object.isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func
};


export default Sensor;