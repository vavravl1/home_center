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

    isTimGranularityBySecond = function () {
        return this.props.timeGranularity === 'BySecond'
    };

    emptyChartData = {
        labels: '',
        times: '',
        datasets: [],
    };

    prepareDatesForChart = () => {
        return this.props.measuredPhenomenons[0].measurements
            .map(t => t.measureTimestamp)
            .map(t => {
                if (this.isTimGranularityByDay()) {
                    return moment.utc(t).format("DD/MM")
                } else if (this.isTimGranularityBySecond()) {
                    return moment(t).format("mm:ss")
                } else {
                    return moment(t).add(1, 'minute').startOf('minute').format("HH:mm")
                }
            });
    };

    prepareExactTimesForChart = () => {
        return this.props.measuredPhenomenons[0].measurements
            .map(t => moment(t.measureTimestamp).add(1, 'minute').format("DD.MM.YYYY HH:mm"));
    };

    chartColors = [
        [255, 0, 0],
        [0, 255, 0],
        [0, 0, 255],
        [127, 0, 127]
    ];

    prepareDataSetForSimpleLineValue = () => {
        return this.props.measuredPhenomenons
            .map(measuredPhenomenon => {
                const averages = measuredPhenomenon.measurements.map(t => t.average);
                const maxes = measuredPhenomenon.measurements.map(t => t.max);
                const index = this.props.measuredPhenomenons.indexOf(measuredPhenomenon);
                const red = this.chartColors[index % 4][0];
                const green = this.chartColors[index % 4][1];
                const blue = this.chartColors[index % 4][2];
                return {
                    label: measuredPhenomenon.name,
                    borderColor: 'rgb(' + red + ',' + green + ',' + blue + ')',
                    data: measuredPhenomenon.aggregationStrategy === 'none' ? averages : maxes,
                }
            });
    };

    prepareDataSetForByDayValues = () => {
        return this.props.measuredPhenomenons
            .map(measuredPhenomenon => {
                const averages = measuredPhenomenon.measurements.map(t => t.average.toFixed(2));
                const maxes = measuredPhenomenon.measurements.map(t => t.max.toFixed(2));
                const mines = measuredPhenomenon.measurements.map(t => t.min.toFixed(2));
                const index = this.props.measuredPhenomenons.indexOf(measuredPhenomenon);
                const red = this.chartColors[index % 4][0];
                const green = this.chartColors[index % 4][1];
                const blue = this.chartColors[index % 4][2];
                return [{
                    label: 'average ' + measuredPhenomenon.name,
                    borderColor: 'rgb(' + red + ',' + green + ',' + blue + ')',
                    data: measuredPhenomenon.aggregationStrategy === 'none' ? averages : maxes,
                }, {
                    label: 'min ' + measuredPhenomenon.name,
                    borderColor: 'rgb(' + ((red + 30) % 255) + ',' + (green + 10) + ',' + (blue - 120) + ')',
                    data: mines,
                }, {
                    label: 'max ' + measuredPhenomenon.name,
                    borderColor: 'rgb(' + ((red - 60) % 255) + ',' + ((green + 60) % 255) + ',' + ((blue - 100) % 255) + ')',
                    data: maxes,
                }]
            }).reduce(function (a, b) {
                return a.concat(b);
            });
    };

    valueChartLineData = function () {
        if (this.props.measuredPhenomenons.length == 0) {
            return this.emptyChartData;
        }

        let datasets = [];
        if (this.isTimGranularityByDay()) {
            datasets = this.prepareDataSetForByDayValues();
        } else {
            datasets = this.prepareDataSetForSimpleLineValue();
        }

        return {
            labels: this.prepareDatesForChart(),
            times: this.prepareExactTimesForChart(),
            datasets: datasets,
        };
    };

    chartOptions = () => {
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
            animation: false,
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
        if (this.props.measuredPhenomenons.length > 0) {
            const lastMeasuredPhenomenon = this.props.measuredPhenomenons[this.props.measuredPhenomenons.length - 1];
            const measurements = lastMeasuredPhenomenon.measurements;
            const lastMeasurement = measurements[measurements.length - 1];
            if (measurements.length > 0) {
                lastTimestamp =
                    <tr key={"lastMeasuredTimestamp-" + lastMeasuredPhenomenon.name + "-" + lastMeasurement.measureTimestamp + "-" + this.props.sensor.name}>
                        <td>Last update</td>
                        <td><Time value={new Date(lastMeasurement.measureTimestamp)} format="HH:mm:ss"/></td>
                    </tr>
            }
        }
        return lastTimestamp;
    };

    prepareLastMeasuredValues = () => {
        return this.props.measuredPhenomenons.map(measuredPhenomenon => {
            const measurements = measuredPhenomenon.measurements;
            if (measurements.length > 0) {
                const lastMeasurement = measurements[measurements.length - 1];
                return <tr key={"lastMeasuredValue-" + measuredPhenomenon.name + "-" + this.props.sensor.name}>
                    <td scope="row">Actual {measuredPhenomenon.name}</td>
                    <td>{lastMeasurement.average.toFixed(2)} {measuredPhenomenon.unit}</td>
                </tr>
            } else {
                return <tr/>
            }
        });
    };

    render = () => {
        const overviews = this.prepareLastMeasuredValues();
        const lastTimestamp = this.prepareLastMeasuredTimestamp();
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
            <Line data={this.valueChartLineData()} options={this.chartOptions()}/>
            <CheckBox
                className="pull-left"
                bsSize="xsmall"
                checked={this.state.startAtZero}
                onChange={this.handleStartAtZero.bind(this)}>Start at zero?</CheckBox>

            <ButtonToolbar className="pull-right">
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'BySecond' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "BySecond")}>By Second</Button>
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
    measuredPhenomenons: PropTypes.arrayOf(PropTypes.object).isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func,
};


export default SensorView;