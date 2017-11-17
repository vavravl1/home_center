import React from "react";
import Time from "react-time";
import {Bar} from "react-chartjs-2";
import moment from "moment";
import ButtonToolbar from "react-bootstrap/lib/ButtonToolbar";
import Button from "react-bootstrap/lib/Button";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import PropTypes from "prop-types";

class SensorBarView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {}
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
                if(this.props.timeGranularity === 'ByDay') {
                    return moment.utc(t).format("DD/MM")
                } else {
                    return moment.utc(t).format("MMM YYYY")
                }
            });
    };

    chartColors = [
        [255, 0, 0],
        [0, 255, 0],
        [0, 0, 255],
        [127, 0, 127]
    ];

    valueChartBarData = function () {
        if (this.props.measuredPhenomenons.length == 0) {
            return this.emptyChartData;
        }

        const datasets = this.props.measuredPhenomenons
            .map(measuredPhenomenon => {
                const maxes = measuredPhenomenon.measurements.map(t => t.max.toFixed(2));
                const index = this.props.measuredPhenomenons.indexOf(measuredPhenomenon);
                const red = this.chartColors[index % 4][0];
                const green = this.chartColors[index % 4][1];
                const blue = this.chartColors[index % 4][2];
                return {
                    label: measuredPhenomenon.name,
                    borderColor: 'rgb(' + red + ',' + green + ',' + blue + ')',
                    backgroundColor: ('rgb(' + red + ',' + green + ',' + blue + ')'),
                    data: maxes,
                }
            });

        return {
            labels: this.prepareDatesForChart(),
            datasets: datasets,
        };
    };

    chartOptions = () => {
        return {
            responsive: true,
            legend: {
                display: true
            },
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

    render = () => {
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
                </tbody>
            </table>
            <Bar data={this.valueChartBarData()} options={this.chartOptions()}/>
            <ButtonToolbar className="pull-right">
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'ByDay' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "ByDay")}>By Day</Button>
                <Button bsSize="xsmall"
                        bsStyle={this.props.timeGranularity === 'ByMonth' ? "primary" : "default"}
                        onClick={this.handleTimeGranularity.bind(this, "ByMonth")}>By Month</Button>
            </ButtonToolbar>
        </Jumbotron>
    };
}

SensorBarView.propTypes = {
    sensor: PropTypes.object.isRequired,
    timeGranularity: PropTypes.string.isRequired,
    timeGranularityChangedCallback: PropTypes.func.isRequired,
    measuredPhenomenons: PropTypes.arrayOf(PropTypes.object).isRequired,
    makeBigCallback: PropTypes.func,
    makeSmallCallback: PropTypes.func,
};


export default SensorBarView;