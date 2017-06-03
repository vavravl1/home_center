import React from "react";
import Time from "react-time";
import {Line} from "react-chartjs-2";
import moment from "moment";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Button from "react-bootstrap/lib/Button";
import CheckBox from "react-bootstrap/lib/CheckBox";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import update from "react-addons-update";
import PropTypes from "prop-types";

class SensorView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            startAtZero: true,
        }
    };

    isTimGranularityByDay = function () {
        return this.props.timeGranularity === 'ByDay'
    };

    valueChartData = function () {
        if (this.props.data.length == 0) {
            return {
                labels: '',
                times: '',
                datasets: [],
            };
        }

        let dates = this.props.data[0].measurements
            .map(t => t.measureTimestamp)
            .map(t => {
                if (this.isTimGranularityByDay()) {
                    return moment(t).startOf('day').format("DD/MM")
                } else {
                    return moment(t).add(1, 'minute').startOf('minute').format("HH:mm")
                }
            });

        let times = this.props.data[0].measurements
            .map(t => moment(t.measureTimestamp).add(1, 'minute').format("DD.MM.YYYY HH:mm"));

        const colors = [
            [166, 166, 166],
            [179, 224, 255],
            [153, 255, 153]
        ];

        let datasets = [];
        if (this.isTimGranularityByDay()) {
            datasets = this.props.data.map(measuredPhenomenon => {
                const averages = measuredPhenomenon.measurements.map(t => t.average);
                const maxes = measuredPhenomenon.measurements.map(t => t.max);
                const mines = measuredPhenomenon.measurements.map(t => t.min);
                const index = this.props.data.indexOf(measuredPhenomenon);
                const red = colors[index % 3][0];
                const green = colors[index % 3][1];
                const blue = colors[index % 3][2];
                return [{
                    label: 'average ' + measuredPhenomenon.name,
                    backgroundColor: 'rgb(' + red+ ',' + green + ',' + blue + ')',
                    borderColor: 'rgb(0, 0, 0)',
                    data: averages,
                    type: measuredPhenomenon.aggregationStrategy === 'none' ? 'line':'bar'
                },{
                    label: 'max ' + measuredPhenomenon.name,
                    backgroundColor: 'rgb(' + (red - 60)+ ',' + (green - 60) + ',' + (blue - 60) + ')',
                    borderColor: 'rgb(0, 0, 0)',
                    data: maxes,
                    type: (measuredPhenomenon.aggregationStrategy === 'none') ? 'line':'bar'
                },{
                    label: 'min ' + measuredPhenomenon.name,
                    backgroundColor: 'rgb(' + (red - 120) + ',' + (green - 120) + ',' + (blue - 120) + ')',
                    borderColor: 'rgb(0, 0, 0)',
                    data: mines,
                    type: measuredPhenomenon.aggregationStrategy === 'none' ? 'line':'bar'
                }]
            }).reduce(function(a, b){
                return a.concat(b);
            });
        } else {
            datasets = this.props.data.map(measuredPhenomenon => {
                const averages = measuredPhenomenon.measurements.map(t => t.average);
                const index = this.props.data.indexOf(measuredPhenomenon);
                const red = colors[index % 3][0];
                const green = colors[index % 3][1];
                const blue = colors[index % 3][2];
                return {
                    label: measuredPhenomenon.name,
                    backgroundColor: 'rgb(' + red+ ',' + green + ',' + blue + ')',
                    borderColor: 'rgb(0, 0, 0)',
                    data: averages,
                    type: measuredPhenomenon.aggregationStrategy === 'none' ? 'line':'line'
                }
            });
        }

        return {
            labels: dates,
            times: times,
            datasets: datasets,
        };
    };

    humidityChartOptions = () => {
        return {
            responsive: true,
            scales: {
                yAxes: [{
                    ticks: {
                        beginAtZero: this.state.startAtZero
                    }
                }]
            },
            legend: {
                display: true
            },
            tooltips: {
                callbacks: {
                    title: function (array, data) {
                        let index = array[0].index;
                        return data.times[index];
                    }
                }
            }
        };
    };

    handleTimeGranularity = function (value) {
        this.props.timeGranularityChangedCallback(value);
    };

    makeBigOrSmallCallback = function () {
        if (!!this.props.makeSmallCallback) {
            this.props.makeSmallCallback();
        } else {
            this.props.makeBigCallback(this.props.sensor);
        }
    };

    handleStartAtZero = function () {
        const newState = update(this.state, {
            startAtZero: {$set: !this.state.startAtZero},
        });
        this.setState(newState);
    };

    prepareLastMeasuredTimestamp = () => {
        let lastTimestamp = <tr/>;
        if (this.props.data.length > 0) {
            const lastMeasuredPhenomenon = this.props.data[this.props.data.length - 1];
            const measurements = lastMeasuredPhenomenon.measurements;
            const lastMeasurement = measurements[measurements.length - 1];
            if (measurements.length > 0) {
                lastTimestamp = <tr key={"lastMeasuredTimestamp-" + lastMeasuredPhenomenon.name + "-" + lastMeasurement.measureTimestamp + "-" + this.props.sensor.name}>
                    <td>Last update</td>
                    <td><Time value={new Date(lastMeasurement.measureTimestamp)} format="HH:mm:ss"/></td>
                </tr>
            }
        }
        return lastTimestamp;
    };

    prepareLastMeasuredValues = () => {
        return this.props.data.map(measuredPhenomenon => {
            const measurements = measuredPhenomenon.measurements;
            if (measurements.length > 0) {
                const lastMeasurement = measurements[measurements.length - 1];
                return <tr key={"lastMeasuredValue-" + measuredPhenomenon.name + "-" + this.props.sensor.name}>
                    <td scope="row">Actual {measuredPhenomenon.name}</td>
                    <td>{lastMeasurement.average} {measuredPhenomenon.unit}</td>
                </tr>
            } else {
                return <tr/>
            }
        });
    };

    render = () => {
        const overviews = this.prepareLastMeasuredValues();
        let lastTimestamp = this.prepareLastMeasuredTimestamp();
        return <Jumbotron bsClass="sensor-measurement-box">
            <h2 className="capital" style={{display: 'inline'}}>{this.props.sensor.name}</h2>
            <ButtonToolbar className="pull-right">
                <Button bsSize="xsmall" className="sensorMakeBig"
                        onClick={this.makeBigOrSmallCallback.bind(this)}>o</Button>
            </ButtonToolbar>
            <h3>{this.props.sensor.location.label}</h3>
            <table className="table table-hover table-bordered table-condensed table-responsive">
                <tbody>
                {lastTimestamp}
                {overviews}
                </tbody>
            </table>
            <Line data={this.valueChartData()} options={this.humidityChartOptions()}/>
            <CheckBox
                className="pull-left"
                bsSize="xsmall"
                checked={this.state.startAtZero}
                onChange={this.handleStartAtZero.bind(this)}>Start at zero?</CheckBox>
            <ButtonToolbar className="pull-right">
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'ByMinute' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "ByMinute")}>By Minute</Button>
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'ByHour' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "ByHour")}>By Hour</Button>
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'ByDay' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "ByDay")}>By Day</Button>
            </ButtonToolbar>
        </Jumbotron>
    };
}

SensorView.propTypes = {
    sensor: PropTypes.object.isRequired,
    timeGranularity: PropTypes.string.isRequired,
    timeGranularityChangedCallback: PropTypes.func.isRequired,
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func,
};


export default SensorView;