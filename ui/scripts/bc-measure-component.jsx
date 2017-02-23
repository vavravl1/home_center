import React, {Component, PropTypes} from "react";
import Time from "react-time";
import {Line} from "react-chartjs-2";
import moment from "moment";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Button from "react-bootstrap/lib/Button";

class BcMeasureComponent extends React.Component {
    isTimGranularityByDay = function() {
        return this.props.activeTimeGranularity === 'ByDay'
    };

    valueChartData = function () {
        let average = this.props.measure.map(t => t.average);
        let min = this.props.measure.map(t => t.min);
        let max = this.props.measure.map(t => t.max);
        let dates = this.props.measure
            .map(t => t.measureTimestamp)
            .map(t => {
                if(this.isTimGranularityByDay()) {
                    return moment(t).startOf('day').format("DD/MM")
                } else {
                    return moment(t).add(1, 'minute').startOf('minute').format("HH:mm")
                }
            });

        let datasets = [];
        if(this.isTimGranularityByDay()) {
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
            datasets =  [{
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
                        beginAtZero: true
                    }
                }]
            },
            legend: {
                display: this.isTimGranularityByDay()
            }
        };
    };

    handleTimeGranularity = function(value) {
        this.props.timeGranularityCallback(value)
    };

    render = () => {
        if (this.props.measure.length > 0) {
            let lastMeasure = this.props.measure[this.props.measure.length - 1];
            return <div className="bc-measurement-box">
                <h2 className="capital">{lastMeasure.sensor}</h2>
                <h3>{this.props.sensorLocation}</h3>
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
                <Line data={this.valueChartData()} options={this.humidityChartOptions()} key={JSON.stringify(this.humidityChartOptions())}/>
                <ButtonToolbar className="pull-right">
                    <Button bsSize="xsmall"
                            bsStyle={this.props.activeTimeGranularity === 'ByMinute' ? "primary" : "default"}
                            onClick={this.handleTimeGranularity.bind(this, "ByMinute")}>By Minute</Button>
                    <Button bsSize="xsmall"
                            bsStyle={this.props.activeTimeGranularity === 'ByHour' ? "primary" : "default"}
                            onClick={this.handleTimeGranularity.bind(this, "ByHour")}>By Hour</Button>
                    <Button bsSize="xsmall"
                            bsStyle={this.props.activeTimeGranularity === 'ByDay' ? "primary" : "default"}
                            onClick={this.handleTimeGranularity.bind(this, "ByDay")}>By Day</Button>
                </ButtonToolbar>
            </div>
        } else {
            return <div className="capital">{this.props.sensorName} is not sending data.</div>
        }
    };
}

BcMeasureComponent.PropTypes = {
    measure: PropTypes.arrayOf(PropTypes.object).isRequired,
    sensorLocation: PropTypes.string.isRequired,
    sensorName: PropTypes.string.isRequired,
    activeTimeGranularity: PropTypes.string.isRequired,
    timeGranularityCallback: PropTypes.func.isRequired
};


export default BcMeasureComponent;