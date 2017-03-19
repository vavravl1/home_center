import React, {Component, PropTypes} from "react";
import Time from "react-time";
import {Line} from "react-chartjs-2";
import moment from "moment";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Button from "react-bootstrap/lib/Button";
import CheckBox from "react-bootstrap/lib/CheckBox";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import Badge from "react-bootstrap/lib/Badge";
import update from "react-addons-update";

class BcSensorView extends React.Component {

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
        let average = this.props.data.map(t => t.average);
        let min = this.props.data.map(t => t.min);
        let max = this.props.data.map(t => t.max);
        let dates = this.props.data
            .map(t => t.measureTimestamp)
            .map(t => {
                if (this.isTimGranularityByDay()) {
                    return moment(t).startOf('day').format("DD/MM")
                } else {
                    return moment(t).add(1, 'minute').startOf('minute').format("HH:mm")
                }
            });

        let datasets = [];
        if (this.isTimGranularityByDay()) {
            datasets = [{
                label: 'Average',
                backgroundColor: 'rgba(240, 247, 254, 0.5)',
                borderColor: 'rgb(0, 0, 0)',
                data: average,
            }, {
                label: 'Min',
                backgroundColor: 'rgba(240, 247, 254, 0.5)',
                borderColor: 'rgb(0, 0, 255)',
                data: min,
            }, {
                label: 'Max',
                backgroundColor: 'rgba(240, 247, 254, 0.5)',
                borderColor: 'rgb(255, 0, 0)',
                data: max,
            }]
        } else {
            datasets = [{
                label: 'Average',
                backgroundColor: 'rgba(240, 247, 254, 0.5)',
                borderColor: 'rgb(0, 0, 0)',
                data: average,
            }]
        }

        return {
            labels: dates,
            datasets: datasets
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
                display: this.isTimGranularityByDay()
            }
        };
    };

    handleTimeGranularity = function (value) {
        this.props.timeGranularityChangedCallback(value);
    };

    makeBigOrSmallCallback = function () {
        if(!!this.props.makeSmallCallback) {
            this.props.makeSmallCallback();
        } else {
            this.props.makeBigCallback(this.props.location, this.props.phenomenon);
        }
    };

    handleStartAtZero = function (value) {
        const newState = update(this.state, {
            startAtZero: {$set: !this.state.startAtZero},
        });
        this.setState(newState);
    };

    render = () => {
        let lastMeasure;
        if (this.props.data.length > 0) {
            lastMeasure = this.props.data[this.props.data.length - 1];
        } else {
            lastMeasure = {
                sensor: this.props.phenomenon,
                location: "n/a",
                measureTimestamp: "n/a",
                phenomenon: "n/a",
                unit: "",
                average: "n/a",
            }
        }
        return <Jumbotron bsClass="bc-measurement-box">
            <h2 className="capital" style={{display: 'inline'}}>{lastMeasure.sensor}</h2>
            <ButtonToolbar className="pull-right">
                <Button bsSize="xsmall" className="bcSensorMakeBig"
                        onClick={this.makeBigOrSmallCallback.bind(this)}>o</Button>
            </ButtonToolbar>
            <h3>{lastMeasure.location}</h3>
            <table className="table table-hover table-bordered table-condensed table-responsive">
                <tbody>
                <tr>
                    <td scope="row">Last update</td>
                    <td><Time value={new Date(lastMeasure.measureTimestamp)} format="HH:mm:ss"/></td>
                </tr>
                <tr>
                    <td scope="row">Actual {lastMeasure.phenomenon}</td>
                    <td>{lastMeasure.average} {lastMeasure.unit}</td>
                </tr>
                </tbody>
            </table>
            <Line data={this.valueChartData()} options={this.humidityChartOptions()}
                  key={JSON.stringify(this.humidityChartOptions())}/>
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

BcSensorView.PropTypes = {
    location: PropTypes.string.isRequired,
    phenomenon: PropTypes.string.isRequired,
    timeGranularity: PropTypes.string.isRequired,
    timeGranularityChangedCallback: PropTypes.func.isRequired,
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func,
};


export default BcSensorView;