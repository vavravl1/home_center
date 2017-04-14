import React, {Component, PropTypes} from "react";
import Button from "react-bootstrap/lib/Button";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Time from "react-time";
import {Bar} from "react-chartjs-2";
import moment from "moment";
import axios from "axios";
import update from "react-addons-update";

class WateringComponent extends React.Component {

    constructor(props) {
        super(props);
        const cancelToken = axios.CancelToken;
        this.state = {
            timeGranularity: "ByHour",
            data: [],
            tickHandler: null,
            source: cancelToken.source()
        }
    };

    componentDidMount = () => {
        this.tick.call(this);
    };

    componentWillUnmount = () => {
        if (this.state.tickHandler !== null) {
            clearTimeout(this.state.tickHandler);
        }
        this.state.source.cancel("Component is not rendered anymore");
    };

    tick = () => {
        let t = this;
        let wateringUrl = document.getElementById('wateringBackendUrl').value;

        axios
            .get(wateringUrl + "?timeGranularity=" + this.state.timeGranularity, {
                cancelToken: this.state.source.token
            })
            .then(function (wateringResponse) {
                const tickHandler = setTimeout(t.tick.bind(t), 1000);
                const newState = update(t.state, {
                    data: {$set: wateringResponse.data},
                    tickHandler: {$set: tickHandler}
                });

                t.setState(newState);

            }).catch(function (error) {
                console.log(error);
        });
    };

    humidityChartData = function () {
        let measures = this.state.data
            .map(w => w.telemetry.humidity.actual);
        let wip = this.state.data
            .map(w => w.telemetry.watering.inProgress ? 10 : 0);
        let dates = this.state.data
            .map(w => w.timestamp)
            .map(w => moment(w).add(1, 'minute').startOf('minute').format("HH:mm:ss"));

        return {
            labels: dates,
            datasets: [
                {
                    type: 'bar',
                    label: 'Watering',
                    backgroundColor: 'rgb(240, 100, 80)',
                    borderColor: 'rgb(0, 0, 0)',
                    data: wip,
                    yAxisID: 'y-watering'
                }, {
                    type: 'line',
                    label: 'Humidity',
                    backgroundColor: 'rgb(240, 247, 254)',
                    borderColor: 'rgb(0, 0, 0)',
                    data: measures,
                    yAxisID: 'y-humidity'
                }
            ]
        };
    };

    humidityChartOptions = function () {
        return {
            responsive: true,
            scales: {
                yAxes: [{
                        type: 'linear',
                        display: false,
                        position: 'left',
                        id: 'y-watering',
                        labels: {
                            show: true
                        },
                        gridLines: {
                            display: false
                        },
                        ticks: {
                            beginAtZero: true,
                            max: 100
                        }
                    }, {
                        type: 'linear',
                        display: true,
                        position: 'right',
                        id: 'y-humidity',
                        labels: {
                            show: true
                        },
                        gridLines: {
                            display: false
                        },
                        ticks: {
                            beginAtZero: true
                        }
                    }
                ]
            }
        }
    };

    handleTimeGranularity = function (value) {
        const newState = update(this.state, {
            timeGranularity: {$set: value},
        });
        this.setState(newState);
    };

    render = () => {
        if (this.state.data.length > 0) {
            let lastWatering = this.state.data[this.state.data.length - 1];
            return <div className="bc-measurement-box">
                <h2>Watering</h2>
                <h3>Ibisek</h3>
                <table className="table table-hover table-bordered table-condensed table-responsive">
                    <tbody>
                    <tr>
                        <td scope="row">Last update</td>
                        <td><Time value={new Date(lastWatering.timestamp)} format="HH:mm:ss"/></td>
                    </tr>
                    <tr>
                        <td scope="row">Humidity</td>
                        <td>{lastWatering.telemetry.humidity.actual}</td>
                    </tr>
                    <tr>
                        <td scope="row">Base line</td>
                        <td>{lastWatering.telemetry.humidity.baseLine}</td>
                    </tr>
                    <tr>
                        <td scope="row">In Progress</td>
                        <td>{lastWatering.telemetry.watering.inProgress ? "Yes" : "No"}</td>
                    </tr>
                    <tr>
                        <td>Water level high</td>
                        <td>{lastWatering.telemetry.waterLevelHigh ? "Yes" : "No"}</td>
                    </tr>
                    </tbody>
                </table>
                <Bar data={this.humidityChartData()} options={this.humidityChartOptions()} />
                <ButtonToolbar >
                    <Button bsSize="small" active onClick={this.manualWatering.bind(this)}>Water!</Button>
                    <div className="pull-right">
                        <Button bsSize="xsmall"
                                bsStyle={this.state.timeGranularity === 'ByMinute' ? "primary" : "default"}
                                onClick={this.handleTimeGranularity.bind(this, "ByMinute")}>By Minute</Button>
                        <Button bsSize="xsmall"
                                bsStyle={this.state.timeGranularity === 'ByHour' ? "primary" : "default"}
                                onClick={this.handleTimeGranularity.bind(this, "ByHour")}>By Hour</Button>
                        <Button bsSize="xsmall"
                                bsStyle={this.state.timeGranularity === 'ByDay' ? "primary" : "default"}
                                onClick={this.handleTimeGranularity.bind(this, "ByDay")}>By Day</Button>
                    </div>
                </ButtonToolbar>
            </div>
        } else {
            return <div>Watering is not sending data {this.props === null ? "x" : "y"}</div>
        }
    };

    manualWatering = function () {
        let wateringBackendUrl = document.getElementById('wateringBackendUrl').value;
        let wateringUrl = wateringBackendUrl + "/manual-watering";
        axios({
            method: 'post',
            url: wateringUrl,
        });
    };
}


export default WateringComponent;