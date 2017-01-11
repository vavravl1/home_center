import React, {Component, PropTypes} from "react";
import Time from "react-time";
import {Line} from "react-chartjs-2";
import moment from "moment";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Button from "react-bootstrap/lib/Button";

class BcMeasureComponent extends React.Component {
    valueChartData = function () {
        let measures = this.props.measure.map(t => t.value);
        let dates = this.props.measure
            .map(t => t.measureTimestamp)
            .map(t => moment(t).add(1, 'minute').startOf('minute').format("HH:mm"));

        return {
            labels: dates,
            datasets: [{
                    backgroundColor: 'rgb(240, 247, 254)',
                    borderColor: 'rgb(0, 0, 0)',
                    data: measures,
            }]
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
                display: false
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
                        <td>{lastMeasure.value} {lastMeasure.unit}</td>
                    </tr>
                    </tbody>
                </table>
                <Line data={this.valueChartData()} options={this.humidityChartOptions()} />
                <ButtonToolbar className="pull-right">
                    <Button bsSize="xsmall" bsStyle={this.props.activeTimeGranularity === 'ByMinute' ? "primary" : "default"} onClick={this.handleTimeGranularity.bind(this, "ByMinute")}>By Minute</Button>
                    <Button bsSize="xsmall" bsStyle={this.props.activeTimeGranularity === 'ByHour' ? "primary" : "default"} onClick={this.handleTimeGranularity.bind(this, "ByHour")}>By Hour</Button>
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