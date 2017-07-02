import React from "react";
import axios from "axios";
import update from "react-addons-update";
import SensorView from "./sensor-view.jsx";
import PropTypes from "prop-types";

class Sensor extends React.Component {

    constructor(props) {
        super(props);
        const cancelToken = axios.CancelToken;
        this.state = {
            timeGranularity: "ByHour",
            data: [],
            tickHandler: null,
            source: cancelToken.source(),
            hiddenMeasuredPhenomenons: []
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

    showHideMeasuredPhenomenon = (phenomenon) => {
        let newState = this.state;

        if (this.state.hiddenMeasuredPhenomenons.indexOf(phenomenon) >= 0) {
            const index = this.state.hiddenMeasuredPhenomenons.indexOf(phenomenon);

            newState = update(this.state, {
                hiddenMeasuredPhenomenons:{
                    $splice: [[index, 1]]
                }
            });
        } else {
            newState = update(this.state, {
                hiddenMeasuredPhenomenons:{
                    $push: [phenomenon]
                }
            });
        }

        this.setState(newState);
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
            .then(function (measurement) {
                const tickHandler = setTimeout(t.tick.bind(t), 1000);
                const newState = update(t.state, {
                    data: {
                        $set: measurement.data.sort((a, b) => {
                            if (a.measurements.length > 0 && b.measurements.length > 0) {
                                return a.measurements[0].average - b.measurements[0].average;
                            } else {
                                return 0;
                            }
                        })
                    },
                    tickHandler: {$set: tickHandler}
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
        return <SensorView
            sensor={this.props.sensor}
            data={this.state.data}
            hiddenMeasuredPhenomenons = {this.state.hiddenMeasuredPhenomenons}
            timeGranularity={this.state.timeGranularity}
            timeGranularityChangedCallback={this.timeGranularityChangedCallback}
            makeBigCallback={this.props.makeBigCallback}
            makeSmallCallback={this.props.makeSmallCallback}
            showHideMeasuredPhenomenon={this.showHideMeasuredPhenomenon}
        />
    };
}

Sensor.propTypes = {
    sensor: PropTypes.object.isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func
};


export default Sensor;