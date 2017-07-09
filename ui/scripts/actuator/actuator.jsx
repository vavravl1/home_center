import React from "react";
import axios from "axios";
import update from "react-addons-update";
import {BootstrapTable, TableHeaderColumn} from "react-bootstrap-table";
import Jumbotron from "react-bootstrap/lib/Jumbotron";
import Col from "react-bootstrap/lib/Col";
import FormGroup from "react-bootstrap/lib/FormGroup";
import FormControl from "react-bootstrap/lib/FormControl";
import ControlLabel from "react-bootstrap/lib/ControlLabel";
import Form from "react-bootstrap/lib/Form";

class Actuator extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            actuators: []
        }
    };

    componentDidMount = () => {
        this.loadData(this);
    };

    loadData = () => {
        let t = this;
        axios.get(document.getElementById('actuatorsUrl').value)
            .then(function (actuators) {
                const newState = update(t.state, {
                    actuators: {
                        $set: actuators.data
                    },
                });
                t.setState(newState);
            }).catch(function (error) {
            console.log(error);
        });
    };

    execute = (locationAddress, actuatorName, commandName) => {
        const commandParameterField = document.getElementById(
            'input_value_' +
            commandName + '_' +
            locationAddress + '_' +
            actuatorName
        );
        console.log(JSON.stringify(commandParameterField));
        window.alert(JSON.stringify(commandParameterField));

        let t = this;
        let postUrl = document.getElementById('actuatorsUrl').value + locationAddress + '/' + actuatorName;
        axios.post(postUrl, {name: commandName, requiredArguments: !!commandParameterField?[commandParameterField]:[]})
            .then(function () {
                t.loadData();
            });

        return false;
    };

    componentWillUnmount = () => {
    };

    requiredArgumentsRenderer = (actuator, cell, command, rowIndex) => {
        if (command.requiredArguments.length == 0) {
            return <button
                type="button"
                onClick={
                    this.execute.bind(
                        this,
                        actuator.location.address,
                        actuator.name,
                        command.name
                    )
                }>
                {command.name}
            </button>
        } else {
            return <Form onSubmit={
                this.execute.bind(
                    this,
                    actuator.location.address,
                    actuator.name,
                    command.name
                )
            }>
                <FormGroup>
                    <ControlLabel>{command.requiredArguments[0].name} [{command.requiredArguments[0].unit}]</ControlLabel>
                    <FormControl
                        id={'input_value_' + command.name + '_' + actuator.location.address + '_' + actuator.name}
                        placeholder={command.requiredArguments[0].value}
                    />
                </FormGroup>
            </Form>
        }
    };

    actuatorView = (actuator) => {
        return <Jumbotron bsClass="bc-measurement-box">
            <h3>{actuator.name}</h3>
            <BootstrapTable data={actuator.supportedCommands}
                            striped
                            hover
                            selectRow={{mode: 'none'}}
            >
                <TableHeaderColumn isKey dataField='name'>Name</TableHeaderColumn>
                <TableHeaderColumn
                    dataField='requiredArguments'
                    dataFormat={this.requiredArgumentsRenderer.bind(this, actuator)}
                    hiddenHeader={true}
                />
            </BootstrapTable>
        </Jumbotron>

    };

    render = () => {
        return <div>
            <Col xs={6} md={5}>
                {this.state.actuators.map(actuator => this.actuatorView(actuator))}
            </Col>
        </div>
    };
}

Actuator.propTypes = {};


export default Actuator;